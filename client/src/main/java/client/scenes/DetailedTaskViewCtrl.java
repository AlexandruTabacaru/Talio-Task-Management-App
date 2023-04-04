package client.scenes;

import client.CustomAlert;
import client.customExceptions.TagException;
import client.customExceptions.TaskException;
import client.utils.*;
import com.google.inject.Inject;
import commons.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.stage.Modality;

import java.io.IOException;
import java.util.Optional;
import java.util.function.Consumer;

public class DetailedTaskViewCtrl {

    @FXML
    private Label taskNameText;
    @FXML
    private TextField taskNameTextField;
    @FXML
    private Label taskDescriptionText;
    @FXML
    private TextArea taskDescriptionTextArea;
    @FXML
    private ListView<SubTask> subTasks;
    @FXML
    private ListView<Tag> tagView;
    @FXML
    private ChoiceBox<Tag> tagChoice;
    private Task task;
    private final CustomAlert customAlert;
    private MainCtrl mainCtrl;
    private final TaskUtils taskUtils;
    private final TaskListUtils taskListUtils;
    private ListCtrl listController;
    private final SubTaskUtils subTaskUtils;
    private final WebSocketUtils webSocketUtils;
    private final TagUtils tagUtils;

    private final Consumer<Task> taskConsumer = (task) -> {
        if(task.id == this.task.id)
            Platform.runLater(this::goBack);
    };
    private final Consumer<TaskList> listConsumer = (list) -> {
        for(Task t : list.getTasks())
            if(t.id == this.task.id)
                Platform.runLater(this::goBack);
    };
    private final Consumer<Task> modifyTaskConsumer = (task) -> {
        Platform.runLater(() -> {
            if(task.id == this.task.id)
                this.setTask(task);
        });
    };


    @Inject
    public DetailedTaskViewCtrl(final TaskListUtils taskListUtils,
                                final TaskUtils taskUtils, final CustomAlert customAlert,
                                final SubTaskUtils subTaskUtils, final TagUtils tagUtils,
                                final WebSocketUtils webSocketUtils) {
        this.taskListUtils = taskListUtils;
        this.taskUtils = taskUtils;
        this.customAlert = customAlert;
        this.subTaskUtils = subTaskUtils;
        this.webSocketUtils = webSocketUtils;
        this.tagUtils = tagUtils;
    }

    public void initialize() {
        this.taskNameTextField.focusedProperty().addListener(((observable, oldValue, newValue) -> {
            if(!newValue){
                try {
                    this.onFocusLostTaskName();
                } catch (TaskException e) {
                    System.out.println(e.getMessage());
                }
            }
        }));
        this.taskDescriptionTextArea.focusedProperty().addListener(((observable,
                                                                     oldValue, newValue) -> {
            if(!newValue){
                try {
                    this.onFocusLostTaskDescription();
                } catch (TaskException e) {
                    System.out.println(e.getMessage());
                }
            }
        }));
    }

    public void registerWebSockets(){
        this.webSocketUtils.registerForTaskMessages("/topic/" + listController.getBoardID() +
                "/" + listController.getTaskList().id + "/deletetask", taskConsumer);
        this.webSocketUtils.registerForListMessages("/topic/" + listController.getBoardID() +
                "/deletelist", listConsumer);
        this.webSocketUtils.registerForTaskMessages("/topic/" + listController.getBoardID() +
                "/" + listController.getTaskList().id + "/modifytask", modifyTaskConsumer);

        Consumer<Tag> addBoardTag = (tag) -> {
            Platform.runLater(() -> {
                if(tagChoice.getItems().contains(tag))
                    return;
                tagChoice.getItems().add(tag);
            });
        };
        Consumer<Tag> deleteBoardTag = (tag) -> {
            Platform.runLater(() -> {
                tagChoice.getItems().remove(tag);
            });
        };
        Consumer<Tag> changeTagConsumer = (tag) -> {
            Platform.runLater(() -> {
                changeTag(tag);
            });
        };
        Consumer<Task> changeTaskTag = (task) -> {
            Platform.runLater(() -> {
                this.task = task;
                tagView.getItems().setAll(task.getTags());
            });
        };
        this.webSocketUtils.registerForTagMessages("/topic/" + listController.getBoardID() +
                "/addtag", addBoardTag);
        this.webSocketUtils.registerForTagMessages("/topic/" + listController.getBoardID() +
                "/deletetag", deleteBoardTag);
        this.webSocketUtils.registerForTagMessages("/topic/" + listController.getBoardID() +
                "/changetag", changeTagConsumer);
        this.webSocketUtils.registerForTaskMessages("/topic/" + listController.getBoardID() +
                "/" + listController.getTaskList().id + "/" + task.id + "/changetasktag",
                changeTaskTag);
    }

    public void changeTag(final Tag tag){
        Tag previous = tagChoice.getItems().stream()
                .filter( t -> t.id == tag.id)
                .findAny().orElse(null);
        if(previous == null)
            return;
        int index = tagChoice.getItems().indexOf(previous);
        if(tagChoice.getItems().remove(previous)){
            tagChoice.getItems().add(index, tag);
        }
        if(tagView.getItems().remove(previous)){
            tagView.getItems().add(index, tag);
        }
    }

    public void onTaskNameClicked(final MouseEvent event){
        if(event.getButton() != MouseButton.PRIMARY)
            return;
        this.taskNameTextField.setText(this.taskNameText.getText());
        this.taskNameTextField.setVisible(true);
        this.taskNameText.setVisible(false);

    }

    public void onTaskDescriptionClicked(final MouseEvent event){
        if(event.getButton() != MouseButton.PRIMARY)
            return;
        this.taskDescriptionTextArea.setText(this.taskDescriptionText.getText());
        this.taskDescriptionTextArea.setVisible(true);
        this.taskDescriptionText.setVisible(false);
    }

    private void onFocusLostTaskName() throws TaskException {
        this.taskNameText.setText(this.taskNameTextField.getText());
        this.taskNameTextField.setVisible(false);
        this.taskNameText.setVisible(true);
        if(this.taskNameTextField.getText().equals(task.getName()))
            return;
        try {
            this.taskUtils.renameTask(listController.getBoardID(), listController.getTaskList().id,
                    task.getId(), this.taskNameTextField.getText());
        } catch (TaskException e) {
            throw new TaskException("Renaming task unsuccessful");
        }
    }

    private void onFocusLostTaskDescription() throws TaskException {
        this.taskDescriptionText.setText(this.taskDescriptionTextArea.getText());
        this.taskDescriptionTextArea.setVisible(false);
        this.taskDescriptionText.setVisible(true);
        if(this.taskDescriptionTextArea.getText().equals(task.getDescription()))
            return;
        try {
            this.taskUtils.editDescription(listController.getBoardID(),
                    listController.getTaskList().id,
                    task.getId(), this.taskDescriptionTextArea.getText());
        } catch (TaskException e) {
            throw new TaskException("Editing the description of the task unsuccessful");
        }
    }

    /**
     * Setter for the task, after it sets the task it will update
     * the fields to the details of the task
     * @param task
     */
    public void setTask(final Task task) {
        this.task = task;
        this.update();
    }

    /**
     * It will update all the fields with the new values from the task
     */
    private void update() {
        taskNameText.setVisible(true);
        taskNameTextField.setVisible(false);
        taskDescriptionText.setVisible(true);
        taskDescriptionTextArea.setVisible(false);
        taskNameText.setText(this.task.getName());

        taskDescriptionText.setText(this.task.getDescription());
        subTasks.setCellFactory(lv -> {
            ListCell<SubTask> cell = new ListCell<>() {
                @Override
                protected void updateItem(final SubTask subTask, final boolean empty) {
                    super.updateItem(subTask, empty);
                    if (subTask == null || empty) {
                        setGraphic(null);
                    } else {
                        try {
                            var cardLoader = new FXMLLoader(getClass().getResource("SubCard.fxml"));
                            Node card = cardLoader.load();
                            SubCardCtrl subCardCtrl = cardLoader.getController();
                            subCardCtrl.initialize(subTask, listController, taskListUtils,
                                    customAlert, subTaskUtils,
                                    DetailedTaskViewCtrl.this);
                            setGraphic(card);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            };
            cell.setOnMouseEntered(event -> {
                lv.getSelectionModel().select(cell.getIndex());
            });
            cell.setOnMouseExited(event -> {
                lv.getSelectionModel().clearSelection(cell.getIndex());
            });
            return cell;
        });

        initializeTagView();
        tagView.getItems().setAll(task.getTags());

        subTasks.getItems().setAll(task.getSubtasks());
    }

    public void initializeTagView() {
        tagView.setCellFactory(lv -> {
            ListCell<Tag> cell = new ListCell<>() {
                @Override
                protected void updateItem(final Tag tag, final boolean empty) {
                    super.updateItem(tag, empty);
                    if (tag == null || empty) {
                        setGraphic(null);
                    } else {
                        try {
                            var cardLoader = new FXMLLoader(getClass()
                                    .getResource("TaskTagCard.fxml"));
                            Node card = cardLoader.load();
                            TaskTagCardCtrl taskTagCardCtrl = cardLoader.getController();
                            taskTagCardCtrl.initialize(tag);
                            setGraphic(card);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            };
            cell.setOnMouseClicked(event -> {
                try {
                    if(cell.getItem() == null)
                        return;
                    tagUtils.deleteTaskTag(listController.getBoardID(),
                            listController.getTaskList().id, task.id, cell.getItem().getId());
                    tagView.getItems().remove(cell.getItem());
                } catch(TagException e) {
                    Alert alert = customAlert.showAlert(e.getMessage());
                    alert.showAndWait();
                }
                event.consume();
            });
            return cell;
        });
    }

    public void addTag() {
        try {
            if(tagChoice.getValue() != null) {
                Tag tag = tagChoice.getValue();
                if(!tagView.getItems().contains(tag)) {
                    tagView.getItems().add(tag);
                    tagUtils.addTaskTag(listController.getBoardID(),
                            listController.getTaskList().getId(), task.id, tag);
                } else {
                    Alert alert = customAlert.showAlert("This tag is already selected");
                    alert.showAndWait();
                }
            }
        } catch(TagException e) {
            Alert alert = customAlert.showAlert(e.getMessage());
            alert.showAndWait();
        }

    }

    private void initializeChoiceBox() {
        try {
            tagChoice.getItems().setAll(tagUtils.getBoardTags(listController.getBoardID()));
            if(!tagChoice.getItems().isEmpty()) {
                tagChoice.setValue(tagChoice.getItems().get(0));
            }
        } catch (TagException e) {
            Alert alert = customAlert.showAlert(e.getMessage());
            alert.showAndWait();
        }
    }

    public void setListController(final ListCtrl listController) {
        this.listController = listController;
        initializeChoiceBox();
    }

    public void goBack() {
        mainCtrl.showBoardCatalogue();
    }

    public boolean deleteTask() {
        try {
            TaskList taskList = listController.getTaskList();
            taskUtils.deleteTask(listController.getBoardID(), taskList.id, task.id);
            mainCtrl.showBoardCatalogue();
            return true;
        } catch (TaskException e){
            Alert alert = customAlert.showAlert(e.getMessage());
            alert.showAndWait();
            return false;
        }
    }


    //This function still needs implementation, should be done by Edsard
    public void addSubTask() {
        TextInputDialog dialog = new TextInputDialog("Name");
        dialog.setTitle("Talio:  Add A Sub Task");
        dialog.setHeaderText("Create A New Sub Task:");
        dialog.setContentText("Name:");

        Optional<String> newName = dialog.showAndWait();
        SubTask subTask;

        if (newName.isPresent()) {
            subTask = makeSubTask(newName.get());
            addCard(subTask);
        }
    }

    public SubTask makeSubTask(final String name) {
        return new SubTask(name, false);
    }

    public void addCard(final SubTask subTask) {
        try {
            if (subTasks == null) {
                subTasks = new ListView<>();
            }
            SubTask updatedSubTask = subTaskUtils.addSubTask(listController.getBoardID(),
                    listController.getTaskList().id, task.id, subTask);
            subTasks.getItems().add(updatedSubTask);
            refreshSubTasks();
        } catch (TaskException e) {
            var alert = new Alert(Alert.AlertType.ERROR);
            alert.initModality(Modality.APPLICATION_MODAL);
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }
    public void refreshSubTasks() throws TaskException {
        Task updatedTask = taskUtils.getTask(listController.getBoardID(),
                listController.getTaskList().id, task.id);
        setTask(updatedTask);
    }
    public Task getTask() {
        return this.task;
    }

    public void setMainCtrl(final MainCtrl mainCtrl) {
        this.mainCtrl = mainCtrl;
    }
}
