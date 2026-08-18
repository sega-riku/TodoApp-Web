package com.sega.todoappweb;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class HomeController {

    private ArrayList<Task> tasks = new ArrayList<>();

    public HomeController() {

        // テストデータ
        tasks.add(new Task(
            "Javaを勉強する",
            LocalDate.of(2026, 8, 5),
            LocalTime.of(10, 0),
            DateType.DEADLINE,
            true
        ));

        tasks.add(new Task(
            "Javaを勉強する",
            LocalDate.of(2026, 8, 5),
            LocalTime.of(10, 0),
            DateType.DEADLINE
        ));

        tasks.add(new Task(
            "Spring Bootを勉強する",
            LocalDate.of(2026, 8, 10),
            LocalTime.of(23, 59),
            DateType.DEADLINE
        ));

        tasks.add(new Task(
            "GitHubへPushする",
            LocalDate.of(2026, 8, 31),
            LocalTime.of(23, 59),
            DateType.DEADLINE
        ));

        tasks.add(new Task(
            "岩手帰る",
            LocalDate.of(2026, 8, 7),
            LocalTime.of(20, 0),
            DateType.SCHEDULE
        ));

        tasks.add(new Task(
            "アプリ完成",
            LocalDate.of(2026, 8, 28),
            LocalTime.of(18, 0),
            DateType.SCHEDULE
        ));

        tasks.add(new Task(
            "アプリ提出",
            LocalDate.of(2026, 8, 31),
            LocalTime.of(18, 0),
            DateType.DEADLINE
        ));
    }

    // メイン画面処理
    @GetMapping("/")
    public String index(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String sort,
        @RequestParam(required = false) String status,
        Model model
    ) {

        // 通常一覧に表示してよいタスク
        ArrayList<Task> visibleTasks = new ArrayList<>();

        // 検索後のタスク
        ArrayList<Task> displayedTasks = new ArrayList<>();

        // ステータス絞り込み後のタスク
        ArrayList<Task> filteredTasks = new ArrayList<>();

        // 右側「今後の予定・締切」用
        ArrayList<Task> upcomingSchedules = new ArrayList<>();
        ArrayList<Task> upcomingDeadlines = new ArrayList<>();

        LocalDateTime now = LocalDateTime.now();

        for (Task task : tasks) {

            LocalDateTime taskDateTime =
                LocalDateTime.of(
                    task.getDeadline(),
                    task.getTime()
                );

            boolean expired =
                task.getDateType() == DateType.DEADLINE
                && taskDateTime.isBefore(now);

            boolean completedExpired =
                expired && task.isCompleted();

            boolean pastSchedule =
                task.getDateType() == DateType.SCHEDULE
                && taskDateTime.isBefore(now);
            
            if(!completedExpired && !pastSchedule){
                visibleTasks.add(task);
            }

        }
        

        if (keyword == null || keyword.isBlank()) {

            displayedTasks.addAll(visibleTasks);

        } else {

            for (Task task : visibleTasks) {
                if (task.getTitle().contains(keyword)) {
                    displayedTasks.add(task);
                }
            }
        }

        if ("deadline".equals(sort)) {

            displayedTasks.sort(
                Comparator.comparing(Task::getDeadline)
                          .thenComparing(Task::getTime)
            );
        }

        if ("completed".equals(status)) {

            for (Task task : displayedTasks) {
                if (task.isCompleted()) {
                    filteredTasks.add(task);
                }
            }

        } else if ("incomplete".equals(status)) {

            for (Task task : displayedTasks) {
                if (!task.isCompleted()) {
                    filteredTasks.add(task);
                }
            }

        } else {

            filteredTasks.addAll(displayedTasks);
        }

        for (Task task : tasks) {
            LocalDateTime taskDateTime =
                LocalDateTime.of(task.getDeadline(),task.getTime());

            if(task.isCompleted()){
                continue;
            }

            if(task.getDateType() == DateType.SCHEDULE){
                if (!taskDateTime.isBefore(now)) {
                    upcomingSchedules.add(task);
                }
            }

            if (task.getDateType() == DateType.DEADLINE) {
                upcomingDeadlines.add(task);
            }
        }

        // 日付順、同じ日なら時間順
        upcomingSchedules.sort(
            Comparator.comparing(Task::getDeadline)
                      .thenComparing(Task::getTime)
        );
        //直近5件の予定
        if (upcomingSchedules.size() > 5) {
            upcomingSchedules = new ArrayList<>(upcomingSchedules.subList(0, 5));
        }

        //締切順
        upcomingDeadlines.sort(
            Comparator.comparing(Task::getDeadline)
                      .thenComparing(Task::getTime)  
        );
        //直近5件の締切(締切超過も込み)
        if(upcomingDeadlines.size() > 5) {
            upcomingDeadlines = new ArrayList<>(upcomingDeadlines.subList(0, 5));
        }

        model.addAttribute("title", "Todo一覧");
        model.addAttribute("tasks", filteredTasks);
        model.addAttribute("allTasks", tasks);

        model.addAttribute("keyword", keyword);
        model.addAttribute("sort", sort);
        model.addAttribute("status", status);

        model.addAttribute("totalCount", visibleTasks.size());
        model.addAttribute("displayedCount", filteredTasks.size());

        model.addAttribute("upcomingSchedules", upcomingSchedules);
        model.addAttribute("upcomingDeadlines", upcomingDeadlines);

        model.addAttribute("today", LocalDate.now());
        model.addAttribute(
            "threeDaysLater",
            LocalDate.now().plusDays(3)
        );

        return "index";
    }

    // タスク追加画面
    @GetMapping("/add")
    public String addTask() {
        return "addTask";
    }

    // タスク追加処理
    @PostMapping("/add")
    public String addTask(
        @RequestParam String title,
        @RequestParam LocalDate deadline,
        @RequestParam LocalTime time,
        @RequestParam DateType dateType
    ) {

        tasks.add(
            new Task(
                title,
                deadline,
                time,
                dateType
            )
        );

        return "redirect:/";
    }

    // タスク完了処理
    @GetMapping("/complete/{index}")
    public String completeTask(@PathVariable int index) {
        tasks.get(index).complete();

        return "redirect:/";
    }

    //タスクを未完了に戻す処理
    @GetMapping("/incomplete/{index}")
    public String incompleteTask(@PathVariable int index) {
        tasks.get(index).incomplete();

        return "redirect:/";
    }
    

    // タスク編集画面
    @GetMapping("/edit/{index}")
    public String editTask(
        @PathVariable int index,
        Model model
    ) {

        Task task = tasks.get(index);

        model.addAttribute("task", task);
        model.addAttribute("index", index);

        return "editTask";
    }

    // タスク更新処理
    @PostMapping("/edit/{index}")
    public String updateTask(
        @PathVariable int index,
        @RequestParam String title,
        @RequestParam LocalDate deadline,
        @RequestParam LocalTime time,
        @RequestParam DateType dateType
    ) {

        Task task = tasks.get(index);

        task.setTitle(title);
        task.setDeadline(deadline);
        task.setTime(time);
        task.setDateType(dateType);

        return "redirect:/";
    }

    // タスク削除処理
    @GetMapping("/delete/{index}")
    public String deleteTask(
        @PathVariable int index
    ) {

        tasks.remove(index);

        return "redirect:/";
    }
}