package com.example.LCS;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class WebController {
    List<Todo> todoList = new CopyOnWriteArrayList<>();
    List<Logging> loggingList = new CopyOnWriteArrayList<>();
    
	@Autowired
	private Diameter diameter;
    
	// Web controller *************************************************************

    @GetMapping("/listTodo")
    public String index(Model model, @RequestParam(value = "limit", required = false) Integer limit) {
        model.addAttribute("todoList", limit != null ? todoList.subList(0, limit) : todoList);
        return "listTodo";
    }

    @GetMapping("/addrequest")
    public String addrequest(Model model) {
        model.addAttribute("todo", new Todo());
        return "addrequest";
    }

    @PostMapping("/addrequest")
    public String addrequest(@ModelAttribute Todo todo) {
        todoList.add(todo);
		diameter.sending(todo.getMSISDN());
        return "success";
    }

    @GetMapping("/logging")
    public String logging(Model model) {
		model.addAttribute("loggingList", loggingList);
        return "logging";
    }
}
