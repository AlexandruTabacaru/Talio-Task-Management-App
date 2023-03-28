package server.api;

import commons.SubTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import server.services.BoardService;
import server.services.SubTaskService;

import java.util.NoSuchElementException;

@RestController
@RequestMapping("api/subtasks")
public class SubTaskController {

    private final SubTaskService subTaskService;

    @Autowired
    public SubTaskController(final SubTaskService subTaskService, final BoardService boardService) {
        this.subTaskService = subTaskService;
        if(boardService.getBoards().isEmpty()){
            boardService.createDefaultBoard();
        }
    }

    @PostMapping("/{boardid}/{listid}/{taskid}/subtask")
    public ResponseEntity<SubTask> addTask(
            @PathVariable("boardid") final long boardid,
            @PathVariable("listid") final long listid,
            @PathVariable("taskid") final long taskid,
            @RequestBody final SubTask subTask
    ) {
        try {
            if (subTask == null || subTask.getName() == null || subTask.getName().equals("")) {
                return ResponseEntity.badRequest().build();
            }
            SubTask createdSubTask = subTaskService.addSubTask(boardid, listid, taskid, subTask);
            return ResponseEntity.ok(createdSubTask);
        }
        catch (NoSuchElementException e) { return ResponseEntity.notFound().build(); }
    }
}
