package server.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import commons.TaskList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import server.services.BoardService;
import server.services.ListService;

import java.util.List;
import java.util.NoSuchElementException;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest(ListController.class)
public class ListControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ListService listService;

    @MockBean
    private BoardService boardService;

    @MockBean
    private SimpMessagingTemplate messages;

    @Test
    public void testGetAllTaskListEndpoint() throws Exception {
        List<TaskList> taskLists = List.of(new TaskList("Test TaskList"),
                new TaskList("TaskList 1"));
        Mockito.when(listService.getLists(1)).thenReturn(taskLists);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/lists/1/tasklists"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name", is("Test TaskList")))
                .andExpect(jsonPath("$[1].name", is("TaskList 1")));
    }

    @Test
    public void testGetTaskListEndpoint() throws Exception {
        TaskList taskList = new TaskList("Test TaskList !");
        Mockito.when(listService.getList(1, 2)).thenReturn(taskList);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/lists/1/tasklist/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("name", is("Test TaskList !")));
    }

    @Test
    public void testDeleteTaskListEndpoint() throws Exception {
        Mockito.when(listService.removeListByID(1, 2)).thenReturn(null);


        mockMvc.perform(MockMvcRequestBuilders.delete("/api/lists/1/2"))
                .andExpect(status().isOk());
        Mockito.verify(listService, Mockito.times(1)).removeListByID(1, 2);
    }

    @Test
    public void testRenameTaskListEndpoint() throws Exception {
        Mockito.when(listService.renameList(1, 2, "Test 2")).thenReturn(null);
        mockMvc.perform(MockMvcRequestBuilders.put("/api/lists/1/2")
                        .param("name", "Test 2"))
                .andExpect(status().isOk());
        Mockito.verify(listService, Mockito.times(1)).renameList(1, 2, "Test 2");
    }

    @Test
    public void testAddTaskListEndpoint() throws Exception {
        TaskList newList = new TaskList("New List");
        String requestBody = new ObjectMapper().writeValueAsString(newList);
        Mockito.when(listService.addList(1, newList)).thenReturn(newList);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/lists/1/tasklist")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("name", is("New List")));
        Mockito.verify(listService, Mockito.times(1)).addList(1, newList);
    }

    @Test
    public void testAddTaskListEndpointBadRequest() throws Exception {
        TaskList newList = new TaskList("");
        String requestBody = new ObjectMapper().writeValueAsString(newList);
        Mockito.when(listService.addList(1, newList)).thenReturn(newList);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/lists/1/tasklist")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isBadRequest());
        Mockito.verify(listService, Mockito.times(0))
            .addList(1, newList);
    }

    @Test
    public void testAddTaskListEndpointNotFound() throws Exception {
        TaskList newList = new TaskList("New List");
        String requestBody = new ObjectMapper().writeValueAsString(newList);
        Mockito.when(listService.addList(1, newList)).thenThrow(NoSuchElementException.class);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/lists/1/tasklist")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isNotFound());
        Mockito.verify(listService, Mockito.times(1))
            .addList(1, newList);
    }

    @Test
    public void getTaskListsEndpointNotFound() throws Exception {
        Mockito.when(listService.getLists(1)).thenThrow(NoSuchElementException.class);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/lists/1/tasklists"))
            .andExpect(status().isNotFound());

        Mockito.verify(listService, Mockito.times(1)).getLists(1);
    }

    @Test
    public void getTaskListEndpointNotFound() throws Exception {
        Mockito.when(listService.getList(1, 2)).thenThrow(NoSuchElementException.class);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/lists/1/tasklist/2"))
            .andExpect(status().isNotFound());

        Mockito.verify(listService, Mockito.times(1)).getList(1, 2);
    }

    @Test
    public void renameTaskListEndpointBadRequest() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.put("/api/lists/1/2")
                .param("name", ""))
            .andExpect(status().isBadRequest());
        Mockito.verify(listService, Mockito.times(0))
            .renameList(1, 2, "");
    }

    @Test
    public void renameTaskListEndpointNotFound() throws Exception {
        Mockito.when(listService.renameList(1, 2, "Name"))
            .thenThrow(NoSuchElementException.class);

        mockMvc.perform(MockMvcRequestBuilders.put("/api/lists/1/2")
                .param("name", "Name"))
            .andExpect(status().isNotFound());

        Mockito.verify(listService, Mockito.times(1))
            .renameList(1, 2, "Name");
    }

    @Test
    public void deleteTaskListEndpointNotFound() throws Exception {
        Mockito.when(listService.removeListByID(1, 2))
            .thenThrow(NoSuchElementException.class);

        mockMvc.perform(MockMvcRequestBuilders.delete("/api/lists/1/2"))
            .andExpect(status().isNotFound());

        Mockito.verify(listService, Mockito.times(1))
            .removeListByID(1, 2);
    }
    @Test
    public void deleteTaskListEndpointInternalServerError() throws Exception {
        Mockito.when(listService.removeListByID(1, 2))
            .thenThrow(RuntimeException.class);

        mockMvc.perform(MockMvcRequestBuilders.delete("/api/lists/1/2"))
            .andExpect(status().isInternalServerError());

        Mockito.verify(listService, Mockito.times(1))
            .removeListByID(1, 2);
    }

    @Test
    public void reorderTasksEndpointOk() throws Exception {
        TaskList taskList = new TaskList("Test TaskList");
        Mockito.when(listService.reorderTask(1L, 2L, 3L, 1))
            .thenReturn(taskList);

        mockMvc.perform(MockMvcRequestBuilders.put("/api/lists/1/2/reorder/3")
                .param("newIndex", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("name", is("Test TaskList")));

        Mockito.verify(listService, Mockito.times(1))
            .reorderTask(1L, 2L, 3L, 1);
    }

    @Test
    public void reorderTaskEndpointNotFound() throws Exception {
        Mockito.when(listService.reorderTask(1L, 2L, 3L, 1))
            .thenThrow(NoSuchElementException.class);

        mockMvc.perform(MockMvcRequestBuilders.put("/api/lists/1/2/reorder/3")
                .param("newIndex", "1"))
            .andExpect(status().isNotFound());

        Mockito.verify(listService, Mockito.times(1))
            .reorderTask(1L, 2L, 3L, 1);
    }
}