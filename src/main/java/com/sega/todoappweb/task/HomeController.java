package com.sega.todoappweb.task;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;

import com.sega.todoappweb.user.User;
import com.sega.todoappweb.user.UserRepository;

@Controller
public class HomeController {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    public HomeController(
        TaskRepository taskRepository,
        UserRepository userRepository
    ) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
    }

    // メイン画面処理
    @GetMapping("/")
    public String index(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String status,
        Model model,
        Principal principal
    ) {

        // ログインユーザー取得処理
        User loginUser =
            userRepository
                .findByUsername(principal.getName())
                .orElseThrow();

        // ログインユーザーのタスク取得処理
        List<Task> tasks =
            taskRepository.findByUser(loginUser);

        // 通常一覧に表示してよいタスク
        ArrayList<Task> visibleTasks = new ArrayList<>();

        // 全件数表示用グループ
        ArrayList<TaskGroup> visibleTaskGroups = new ArrayList<>();

        // 検索後のタスク
        ArrayList<Task> displayedTasks = new ArrayList<>();

        // ステータス絞り込み後のタスク
        ArrayList<Task> filteredTasks = new ArrayList<>();

        // 左側のタスク一覧をグループ化
        ArrayList<TaskGroup> taskGroups = new ArrayList<>();

        // 右側「今後の予定・締切」用
        ArrayList<Task> upcomingSchedules = new ArrayList<>();
        ArrayList<Task> upcomingDeadlines = new ArrayList<>();

        ArrayList<TaskGroup> scheduleGroups = new ArrayList<>();
        ArrayList<TaskDateGroup> scheduleDateGroups = new ArrayList<>();

        ArrayList<TaskGroup> deadlineGroups = new ArrayList<>();
        ArrayList<TaskDateGroup> deadlineDateGroups = new ArrayList<>();

        LocalDateTime now = LocalDateTime.now();

        // 左側に表示してよいタスク
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

            if (!completedExpired && !pastSchedule) {
                visibleTasks.add(task);
            }
        }

        // 同じ日付・同じタイトル・同じ予定/締切
        for (Task task : visibleTasks) {

            TaskGroup foundGroup = null;

            for (TaskGroup group : visibleTaskGroups) {

                boolean sameDate =
                    group.getDeadline()
                         .equals(task.getDeadline());

                boolean sameTitle =
                    group.getTitle()
                         .equals(task.getTitle());

                boolean sameDateType =
                    group.getTasks()
                         .get(0)
                         .getDateType()
                    == task.getDateType();

                if (
                    sameDate
                    && sameTitle
                    && sameDateType
                ) {
                    foundGroup = group;
                    break;
                }
            }

            if (foundGroup != null) {

                foundGroup.addTask(task);

            } else {

                TaskGroup newGroup =
                    new TaskGroup(
                        task.getDeadline(),
                        task.getTitle()
                    );

                newGroup.addTask(task);

                visibleTaskGroups.add(newGroup);
            }
        }

        // 検索
        if (keyword == null || keyword.isBlank()) {

            displayedTasks.addAll(visibleTasks);

        } else {

            for (Task task : visibleTasks) {

                if (task.getTitle().contains(keyword)) {
                    displayedTasks.add(task);
                }
            }
        }

        // 日付順、同じ日なら時間順
        displayedTasks.sort(
            Comparator.comparing(Task::getDeadline)
                      .thenComparing(Task::getTime)
        );

        // ステータス絞り込み
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

        // 左側のタスクをグループ化
        for (Task task : filteredTasks) {

            TaskGroup foundGroup = null;

            for (TaskGroup group : taskGroups) {

                boolean sameDate =
                    group.getDeadline()
                         .equals(task.getDeadline());

                boolean sameTitle =
                    group.getTitle()
                         .equals(task.getTitle());

                boolean sameDateType =
                    group.getTasks()
                         .get(0)
                         .getDateType()
                    == task.getDateType();

                if (
                    sameDate
                    && sameTitle
                    && sameDateType
                ) {
                    foundGroup = group;
                    break;
                }
            }

            if (foundGroup != null) {

                foundGroup.addTask(task);

            } else {

                TaskGroup newGroup =
                    new TaskGroup(
                        task.getDeadline(),
                        task.getTitle()
                    );

                newGroup.addTask(task);

                taskGroups.add(newGroup);
            }
        }

        // 右側に表示する予定・締切
        for (Task task : tasks) {

            LocalDateTime taskDateTime =
                LocalDateTime.of(
                    task.getDeadline(),
                    task.getTime()
                );

            // 完了済みは右側に表示しない
            if (task.isCompleted()) {
                continue;
            }

            // 今後の予定
            if (task.getDateType() == DateType.SCHEDULE) {

                if (!taskDateTime.isBefore(now)) {
                    upcomingSchedules.add(task);
                }
            }

            // 締切
            if (task.getDateType() == DateType.DEADLINE) {
                upcomingDeadlines.add(task);
            }
        }

        // 予定
        upcomingSchedules.sort(
            Comparator.comparing(Task::getDeadline)
                      .thenComparing(Task::getTime)
        );

        // 同じ日付・同じタスク名でグループ化
        for (Task task : upcomingSchedules) {

            TaskGroup foundGroup = null;

            for (TaskGroup group : scheduleGroups) {

                boolean sameDate =
                    group.getDeadline()
                         .equals(task.getDeadline());

                boolean sameTitle =
                    group.getTitle()
                         .equals(task.getTitle());

                if (sameDate && sameTitle) {
                    foundGroup = group;
                    break;
                }
            }

            if (foundGroup != null) {

                foundGroup.addTask(task);

            } else {

                TaskGroup newGroup =
                    new TaskGroup(
                        task.getDeadline(),
                        task.getTitle()
                    );

                newGroup.addTask(task);

                scheduleGroups.add(newGroup);
            }
        }

        // 予定をさらに同じ日付でグループ化
        for (TaskGroup group : scheduleGroups) {

            TaskDateGroup foundDateGroup = null;

            for (TaskDateGroup dateGroup : scheduleDateGroups) {

                if (
                    dateGroup.getDeadline()
                             .equals(group.getDeadline())
                ) {
                    foundDateGroup = dateGroup;
                    break;
                }
            }

            if (foundDateGroup != null) {

                foundDateGroup.addGroup(group);

            } else {

                TaskDateGroup newDateGroup =
                    new TaskDateGroup(
                        group.getDeadline()
                    );

                newDateGroup.addGroup(group);

                scheduleDateGroups.add(newDateGroup);
            }
        }

        // 予定は直近5日分まで表示
        if (scheduleDateGroups.size() > 5) {

            scheduleDateGroups =
                new ArrayList<>(
                    scheduleDateGroups.subList(0, 5)
                );
        }

        // 締切
        upcomingDeadlines.sort(
            Comparator.comparing(Task::getDeadline)
                      .thenComparing(Task::getTime)
        );

        // 同じ日付・同じタスク名でグループ化
        for (Task task : upcomingDeadlines) {

            TaskGroup foundGroup = null;

            for (TaskGroup group : deadlineGroups) {

                boolean sameDate =
                    group.getDeadline()
                         .equals(task.getDeadline());

                boolean sameTitle =
                    group.getTitle()
                         .equals(task.getTitle());

                if (sameDate && sameTitle) {
                    foundGroup = group;
                    break;
                }
            }

            if (foundGroup != null) {

                foundGroup.addTask(task);

            } else {

                TaskGroup newGroup =
                    new TaskGroup(
                        task.getDeadline(),
                        task.getTitle()
                    );

                newGroup.addTask(task);

                deadlineGroups.add(newGroup);
            }
        }

        // 締切をさらに同じ日付でグループ化
        for (TaskGroup group : deadlineGroups) {

            TaskDateGroup foundDateGroup = null;

            for (TaskDateGroup dateGroup : deadlineDateGroups) {

                if (
                    dateGroup.getDeadline()
                             .equals(group.getDeadline())
                ) {
                    foundDateGroup = dateGroup;
                    break;
                }
            }

            if (foundDateGroup != null) {

                foundDateGroup.addGroup(group);

            } else {

                TaskDateGroup newDateGroup =
                    new TaskDateGroup(
                        group.getDeadline()
                    );

                newDateGroup.addGroup(group);

                deadlineDateGroups.add(newDateGroup);
            }
        }

        // 締切は直近5日分まで表示
        if (deadlineDateGroups.size() > 5) {

            deadlineDateGroups =
                new ArrayList<>(
                    deadlineDateGroups.subList(0, 5)
                );
        }

        model.addAttribute(
            "title",
            "Todo一覧"
        );

        model.addAttribute(
            "tasks",
            filteredTasks
        );

        model.addAttribute(
            "taskGroups",
            taskGroups
        );

        model.addAttribute(
            "allTasks",
            tasks
        );

        model.addAttribute(
            "keyword",
            keyword
        );

        model.addAttribute(
            "status",
            status
        );

        // 全件数もグループ数
        model.addAttribute(
            "totalCount",
            visibleTaskGroups.size()
        );

        // 表示中件数もグループ数
        model.addAttribute(
            "displayedCount",
            taskGroups.size()
        );

        model.addAttribute(
            "upcomingSchedules",
            upcomingSchedules
        );

        model.addAttribute(
            "upcomingDeadlines",
            upcomingDeadlines
        );

        model.addAttribute(
            "scheduleGroups",
            scheduleGroups
        );

        model.addAttribute(
            "scheduleDateGroups",
            scheduleDateGroups
        );

        model.addAttribute(
            "deadlineGroups",
            deadlineGroups
        );

        model.addAttribute(
            "deadlineDateGroups",
            deadlineDateGroups
        );

        model.addAttribute(
            "today",
            LocalDate.now()
        );

        model.addAttribute(
            "threeDaysLater",
            LocalDate.now().plusDays(3)
        );

        // ログインユーザー名表示処理
        model.addAttribute(
            "username",
            loginUser.getUsername()
        );

        // 管理者判定処理
        boolean isAdmin =
            "ADMIN".equals(loginUser.getRole());

        model.addAttribute(
            "isAdmin",
            isAdmin
        );

        return "task/index";
    }

    // タスク追加画面
    @GetMapping("/add")
    public String addTask() {

        return "task/addTask";
    }

    // タスク追加処理
    @PostMapping("/add")
    public String addTask(
        @RequestParam String title,
        @RequestParam LocalDate deadline,
        @RequestParam LocalTime time,
        @RequestParam DateType dateType,
        @RequestParam(required = false) String description,
        Principal principal
    ) {

        // ログインユーザー取得処理
        User loginUser =
            userRepository
                .findByUsername(principal.getName())
                .orElseThrow();

        Task task =
            new Task(
                title,
                deadline,
                time,
                dateType,
                description
            );

        // タスク所有ユーザー設定
        task.setUser(loginUser);

        taskRepository.save(task);

        return "redirect:/";
    }

    // 既存の予定に時間・詳細を追加する画面
    @GetMapping("/schedule/add/{id}")
    public String addScheduleTime(
        @PathVariable Long id,
        Model model,
        Principal principal
    ) {

        // ログインユーザー取得処理
        User loginUser =
            userRepository
                .findByUsername(principal.getName())
                .orElseThrow();

        Task task =
            taskRepository
                .findById(id)
                .orElseThrow();

        // タスク所有ユーザー確認処理
        if (
            task.getUser() == null
            || !task.getUser()
                    .getId()
                    .equals(loginUser.getId())
        ) {
            return "redirect:/";
        }

        model.addAttribute(
            "task",
            task
        );

        return "task/addScheduleTime";
    }

    // 既存の予定に時間・詳細を追加する処理
    @PostMapping("/schedule/add/{id}")
    public String addScheduleTime(
        @PathVariable Long id,
        @RequestParam LocalTime time,
        @RequestParam(required = false) String description,
        Principal principal
    ) {

        // ログインユーザー取得処理
        User loginUser =
            userRepository
                .findByUsername(principal.getName())
                .orElseThrow();

        Task baseTask =
            taskRepository
                .findById(id)
                .orElseThrow();

        // タスク所有ユーザー確認処理
        if (
            baseTask.getUser() == null
            || !baseTask.getUser()
                        .getId()
                        .equals(loginUser.getId())
        ) {
            return "redirect:/";
        }

        Task newTask =
            new Task(
                baseTask.getTitle(),
                baseTask.getDeadline(),
                time,
                DateType.SCHEDULE,
                description
            );

        // タスク所有ユーザー設定
        newTask.setUser(loginUser);

        taskRepository.save(newTask);

        return "redirect:/";
    }

    // 個別タスク完了処理
    @GetMapping("/complete/{id}")
    public String completeTask(
        @PathVariable Long id,
        Principal principal
    ) {

        // ログインユーザー取得処理
        User loginUser =
            userRepository
                .findByUsername(principal.getName())
                .orElseThrow();

        Task task =
            taskRepository
                .findById(id)
                .orElseThrow();

        // タスク所有ユーザー確認処理
        if (
            task.getUser() == null
            || !task.getUser()
                    .getId()
                    .equals(loginUser.getId())
        ) {
            return "redirect:/";
        }

        task.complete();

        taskRepository.save(task);

        return "redirect:/";
    }

    // 個別タスクを未完了に戻す処理
    @GetMapping("/incomplete/{id}")
    public String incompleteTask(
        @PathVariable Long id,
        Principal principal
    ) {

        // ログインユーザー取得処理
        User loginUser =
            userRepository
                .findByUsername(principal.getName())
                .orElseThrow();

        Task task =
            taskRepository
                .findById(id)
                .orElseThrow();

        // タスク所有ユーザー確認処理
        if (
            task.getUser() == null
            || !task.getUser()
                    .getId()
                    .equals(loginUser.getId())
        ) {
            return "redirect:/";
        }

        task.incomplete();

        taskRepository.save(task);

        return "redirect:/";
    }

    // グループ一括完了処理
    @GetMapping("/complete/group/{id}")
    public String completeTaskGroup(
        @PathVariable Long id,
        Principal principal
    ) {

        // ログインユーザー取得処理
        User loginUser =
            userRepository
                .findByUsername(principal.getName())
                .orElseThrow();

        Task baseTask =
            taskRepository
                .findById(id)
                .orElseThrow();

        // タスク所有ユーザー確認処理
        if (
            baseTask.getUser() == null
            || !baseTask.getUser()
                        .getId()
                        .equals(loginUser.getId())
        ) {
            return "redirect:/";
        }

        // ログインユーザーのタスク取得処理
        List<Task> tasks =
            taskRepository.findByUser(loginUser);

        for (Task task : tasks) {

            boolean sameDate =
                task.getDeadline()
                    .equals(baseTask.getDeadline());

            boolean sameTitle =
                task.getTitle()
                    .equals(baseTask.getTitle());

            boolean sameDateType =
                task.getDateType()
                == baseTask.getDateType();

            if (
                sameDate
                && sameTitle
                && sameDateType
            ) {

                task.complete();

                taskRepository.save(task);
            }
        }

        return "redirect:/";
    }

    // グループ一括未完了処理
    @GetMapping("/incomplete/group/{id}")
    public String incompleteTaskGroup(
        @PathVariable Long id,
        Principal principal
    ) {

        // ログインユーザー取得処理
        User loginUser =
            userRepository
                .findByUsername(principal.getName())
                .orElseThrow();

        Task baseTask =
            taskRepository
                .findById(id)
                .orElseThrow();

        // タスク所有ユーザー確認処理
        if (
            baseTask.getUser() == null
            || !baseTask.getUser()
                        .getId()
                        .equals(loginUser.getId())
        ) {
            return "redirect:/";
        }

        // ログインユーザーのタスク取得処理
        List<Task> tasks =
            taskRepository.findByUser(loginUser);

        for (Task task : tasks) {

            boolean sameDate =
                task.getDeadline()
                    .equals(baseTask.getDeadline());

            boolean sameTitle =
                task.getTitle()
                    .equals(baseTask.getTitle());

            boolean sameDateType =
                task.getDateType()
                == baseTask.getDateType();

            if (
                sameDate
                && sameTitle
                && sameDateType
            ) {

                task.incomplete();

                taskRepository.save(task);
            }
        }

        return "redirect:/";
    }

    // タスク編集画面
    @GetMapping("/edit/{id}")
    public String editTask(
        @PathVariable Long id,
        Model model,
        Principal principal
    ) {

        // ログインユーザー取得処理
        User loginUser =
            userRepository
                .findByUsername(principal.getName())
                .orElseThrow();

        Task task =
            taskRepository
                .findById(id)
                .orElseThrow();

        // タスク所有ユーザー確認処理
        if (
            task.getUser() == null
            || !task.getUser()
                    .getId()
                    .equals(loginUser.getId())
        ) {
            return "redirect:/";
        }

        model.addAttribute(
            "task",
            task
        );

        model.addAttribute(
            "index",
            id
        );

        return "task/editTask";
    }

    // タスク更新処理
    @PostMapping("/edit/{id}")
    public String updateTask(
        @PathVariable Long id,
        @RequestParam String title,
        @RequestParam LocalDate deadline,
        @RequestParam LocalTime time,
        @RequestParam DateType dateType,
        @RequestParam(required = false) String description,
        Principal principal
    ) {

        // ログインユーザー取得処理
        User loginUser =
            userRepository
                .findByUsername(principal.getName())
                .orElseThrow();

        Task task =
            taskRepository
                .findById(id)
                .orElseThrow();

        // タスク所有ユーザー確認処理
        if (
            task.getUser() == null
            || !task.getUser()
                    .getId()
                    .equals(loginUser.getId())
        ) {
            return "redirect:/";
        }

        task.setTitle(title);
        task.setDeadline(deadline);
        task.setTime(time);
        task.setDateType(dateType);
        task.setDescription(description);

        taskRepository.save(task);

        return "redirect:/";
    }

    // タスク削除処理
    @GetMapping("/delete/{id}")
    public String deleteTask(
        @PathVariable Long id,
        Principal principal
    ) {

        // ログインユーザー取得処理
        User loginUser =
            userRepository
                .findByUsername(principal.getName())
                .orElseThrow();

        Task task =
            taskRepository
                .findById(id)
                .orElseThrow();

        // タスク所有ユーザー確認処理
        if (
            task.getUser() == null
            || !task.getUser()
                    .getId()
                    .equals(loginUser.getId())
        ) {
            return "redirect:/";
        }

        taskRepository.deleteById(id);

        return "redirect:/";
    }
}