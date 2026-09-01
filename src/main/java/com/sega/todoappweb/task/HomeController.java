package com.sega.todoappweb.task;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

import com.sega.todoappweb.user.User;
import com.sega.todoappweb.user.UserRepository;
import com.sega.todoappweb.contact.ContactRepository;

@Controller
public class HomeController {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final ContactRepository contactRepository;

    public HomeController(
        TaskRepository taskRepository,
        UserRepository userRepository,
        ContactRepository contactRepository
    ) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.contactRepository = contactRepository;
    }

    // メイン画面処理
    @GetMapping("/")
    public String index(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) Boolean newReply,
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
        ArrayList<Task> visibleTasks =
            new ArrayList<>();

        // 全件数表示用グループ
        ArrayList<TaskGroup> visibleTaskGroups =
            new ArrayList<>();

        // 検索後のタスク
        ArrayList<Task> displayedTasks =
            new ArrayList<>();

        // ステータス絞り込み後のタスク
        ArrayList<Task> filteredTasks =
            new ArrayList<>();

        // タスク一覧をグループ化
        ArrayList<TaskGroup> taskGroups =
            new ArrayList<>();

        // 「予定・締切」用
        ArrayList<Task> upcomingSchedules =
            new ArrayList<>();

        ArrayList<Task> upcomingDeadlines =
            new ArrayList<>();

        ArrayList<TaskGroup> scheduleGroups =
            new ArrayList<>();

        ArrayList<TaskDateGroup> scheduleDateGroups =
            new ArrayList<>();

        ArrayList<TaskGroup> deadlineGroups =
            new ArrayList<>();

        ArrayList<TaskDateGroup> deadlineDateGroups =
            new ArrayList<>();

        // 予定・締切グループのステータス
        Map<Long, String> groupStatuses =
            new HashMap<>();

        // タスク一覧グループのステータス
        Map<Long, String> taskListGroupStatuses =
            new HashMap<>();

        LocalDateTime now =
            LocalDateTime.now();

        LocalDate today =
            LocalDate.now();
        
        LocalDate sevenDaysLater = today.plusDays(6);

        // 通常一覧に表示してよいタスク
        for (Task task : tasks) {

            boolean expired =
                task.isExpired();

            boolean completedExpired =
                expired
                && task.isCompleted();

            boolean pastSchedule = false;

            //予定の過去判定処理
            if(task.getDateType() == DateType.SCHEDULE){
                LocalTime groupStartTime = task.getGroupStartTime();
                LocalTime groupEndTime = task.getGroupEndTime();
                
                //予定全体の終了時間がある場合
                if(groupEndTime != null){
                    LocalDateTime taskDateTime = LocalDateTime.of(task.getDeadline(),groupEndTime);
                    pastSchedule = taskDateTime.isBefore(now);

                //予定全体の開始時間だけある場合
                }else if(groupStartTime != null){
                    LocalDateTime taskDateTime = LocalDateTime.of(task.getDeadline(),groupStartTime);
                    pastSchedule = taskDateTime.isBefore(now);
                
                //予定全体の時間がない場合
                }else{
                    pastSchedule = task.getDeadline().isBefore(today);
                }
            }

            if (
                !completedExpired
                && !pastSchedule
            ) {

                visibleTasks.add(task);
            }
        }

        // 同じ日付・同じタイトル・同じ予定/締切
        for (Task task : visibleTasks) {

            TaskGroup foundGroup =
                null;

            for (
                TaskGroup group :
                visibleTaskGroups
            ) {

                boolean sameDate =
                    group.getDeadline()
                        .equals(
                            task.getDeadline()
                        );

                boolean sameTitle =
                    group.getTitle()
                        .equals(
                            task.getTitle()
                        );

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

                    foundGroup =
                        group;

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

                visibleTaskGroups.add(
                    newGroup
                );
            }
        }

        // 検索
        if (
            keyword == null
            || keyword.isBlank()
        ) {

            displayedTasks.addAll(
                visibleTasks
            );

        } else {

            for (Task task : visibleTasks) {

                if (
                    task.getTitle()
                        .contains(keyword)
                ) {

                    displayedTasks.add(
                        task
                    );
                }
            }
        }

        // 日付順、同じ日なら時間順
        // 時間未設定は同じ日付内の最後に表示
        displayedTasks.sort(
            Comparator
                .comparing(
                    Task::getDeadline
                )
                .thenComparing(
                    Task::getTime,
                    Comparator.nullsLast(
                        Comparator.naturalOrder()
                    )
                )
        );

        // ステータス絞り込み
        if ("completed".equals(status)) {

            for (Task task : displayedTasks) {

                if (task.isCompleted()) {

                    filteredTasks.add(task);
                }
            }

        } else if (
            "incomplete".equals(status)
        ) {

            for (Task task : displayedTasks) {

                if (!task.isCompleted()) {

                    filteredTasks.add(task);
                }
            }

        } else {

            filteredTasks.addAll(
                displayedTasks
            );
        }

        // タスク一覧をグループ化
        for (Task task : filteredTasks) {

            TaskGroup foundGroup =
                null;

            for (TaskGroup group : taskGroups) {

                boolean sameDate =
                    group.getDeadline()
                        .equals(
                            task.getDeadline()
                        );

                boolean sameTitle =
                    group.getTitle()
                        .equals(
                            task.getTitle()
                        );

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

                    foundGroup =
                        group;

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

                taskGroups.add(
                    newGroup
                );
            }
        }


        //タスクグループを日付・全体時間順に並び替え
        taskGroups.sort(
            Comparator
                .comparing(
                TaskGroup::getDeadline
            )
            .thenComparing(
                group -> group.getTasks()
                    .get(0)
                    .getGroupStartTime(),
                Comparator.nullsLast(
                    Comparator.naturalOrder()
                )
            )
        );

        // 「予定・締切」に表示するタスク
        for (Task task : tasks) {

            if (
                task.getDateType()
                == DateType.SCHEDULE
            ) {

                // 今日より前の日付の予定は対象外
                if (
                    !task.getDeadline().isBefore(today)
                    && !task.getDeadline().isAfter(sevenDaysLater)
                ) {

                    upcomingSchedules.add(
                        task
                    );
                }
            }

            if (
                task.getDateType()
                == DateType.DEADLINE
            ) {
                if (
                    !task.getDeadline().isBefore(today)
                    && !task.getDeadline().isAfter(sevenDaysLater)
                    ) {
                    upcomingDeadlines.add(
                    task
                );
                }
            }
        }

        // 予定
        // 日付順、同じ日なら時間順
        // 時間未設定は同じ日付内の最後に表示
        upcomingSchedules.sort(
            Comparator
                .comparing(
                    Task::getDeadline
                )
                .thenComparing(
                    Task::getGroupStartTime,
                    Comparator.nullsLast(
                        Comparator.naturalOrder()
                    )
                )
        );

        // 同じ日付・同じタスク名でグループ化
        for (Task task : upcomingSchedules) {

            TaskGroup foundGroup =
                null;

            for (
                TaskGroup group :
                scheduleGroups
            ) {

                boolean sameDate =
                    group.getDeadline()
                        .equals(
                            task.getDeadline()
                        );

                boolean sameTitle =
                    group.getTitle()
                        .equals(
                            task.getTitle()
                        );

                if (
                    sameDate
                    && sameTitle
                ) {

                    foundGroup =
                        group;

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

                scheduleGroups.add(
                    newGroup
                );
            }
        }

        // 予定グループの表示判定
        ArrayList<TaskGroup> visibleScheduleGroups =
            new ArrayList<>();

        for (TaskGroup group : scheduleGroups) {

            boolean allCompleted =
                true;

            boolean hasUpcomingIncompleteTask =
                false;

            for (Task task : group.getTasks()) {

                if (!task.isCompleted()) {

                    allCompleted =
                        false;

                    boolean upcomingSchedule;

                    LocalTime groupStartTime = task.getGroupStartTime();
                    LocalTime groupEndTime = task.getGroupEndTime();

                    //予定全体の終了時間がある場合
                    if(groupEndTime != null){
                        LocalDateTime taskDateTime = LocalDateTime.of(task.getDeadline(), groupEndTime);

                        upcomingSchedule = !taskDateTime.isBefore(now);
                    
                    //予定全体の開始時間だけある場合
                    }else if(groupStartTime != null){
                        LocalDateTime taskDateTime = LocalDateTime.of(task.getDeadline(),groupStartTime);

                        upcomingSchedule = !taskDateTime.isBefore(now);

                    //予定全体の時間がない場合
                    }else{
                        upcomingSchedule = !task.getDeadline().isBefore(today);
                    }

                    if (upcomingSchedule) {
                        hasUpcomingIncompleteTask = true;
                    }
                }
            }

            if (
                !allCompleted
                && hasUpcomingIncompleteTask
            ) {

                visibleScheduleGroups.add(
                    group
                );
            }
        }

        scheduleGroups =
            visibleScheduleGroups;

        // 予定をさらに同じ日付でグループ化
        for (
            TaskGroup group :
            scheduleGroups
        ) {

            TaskDateGroup foundDateGroup =
                null;

            for (
                TaskDateGroup dateGroup :
                scheduleDateGroups
            ) {

                if (
                    dateGroup.getDeadline()
                        .equals(
                            group.getDeadline()
                        )
                ) {

                    foundDateGroup =
                        dateGroup;

                    break;
                }
            }

            if (foundDateGroup != null) {

                foundDateGroup.addGroup(
                    group
                );

            } else {

                TaskDateGroup newDateGroup =
                    new TaskDateGroup(
                        group.getDeadline()
                    );

                newDateGroup.addGroup(
                    group
                );

                scheduleDateGroups.add(
                    newDateGroup
                );
            }
        }

        // 締切
        // 日付順、同じ日なら時間順
        // 時間未設定は同じ日付内の最後に表示
        upcomingDeadlines.sort(
            Comparator
                .comparing(
                    Task::getDeadline
                )
                .thenComparing(
                    Task::getGroupStartTime,
                    Comparator.nullsLast(
                        Comparator.naturalOrder()
                    )
                )
        );

        // 同じ日付・同じタスク名でグループ化
        for (Task task : upcomingDeadlines) {

            TaskGroup foundGroup =
                null;

            for (
                TaskGroup group :
                deadlineGroups
            ) {

                boolean sameDate =
                    group.getDeadline()
                        .equals(
                            task.getDeadline()
                        );

                boolean sameTitle =
                    group.getTitle()
                        .equals(
                            task.getTitle()
                        );

                if (
                    sameDate
                    && sameTitle
                ) {

                    foundGroup =
                        group;

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

                deadlineGroups.add(
                    newGroup
                );
            }
        }

        // 締切グループの表示判定
        ArrayList<TaskGroup> visibleDeadlineGroups =
            new ArrayList<>();

        for (TaskGroup group : deadlineGroups) {

            boolean allCompleted =
                true;

            for (Task task : group.getTasks()) {

                if (!task.isCompleted()) {

                    allCompleted =
                        false;

                    break;
                }
            }

            if (!allCompleted) {

                visibleDeadlineGroups.add(
                    group
                );
            }
        }

        deadlineGroups =
            visibleDeadlineGroups;

        // 締切をさらに同じ日付でグループ化
        for (
            TaskGroup group :
            deadlineGroups
        ) {

            TaskDateGroup foundDateGroup =
                null;

            for (
                TaskDateGroup dateGroup :
                deadlineDateGroups
            ) {

                if (
                    dateGroup.getDeadline()
                        .equals(
                            group.getDeadline()
                        )
                ) {

                    foundDateGroup =
                        dateGroup;

                    break;
                }
            }

            if (foundDateGroup != null) {

                foundDateGroup.addGroup(
                    group
                );

            } else {

                TaskDateGroup newDateGroup =
                    new TaskDateGroup(
                        group.getDeadline()
                    );

                newDateGroup.addGroup(
                    group
                );

                deadlineDateGroups.add(
                    newDateGroup
                );
            }
        }

        // グループステータス判定
        // 予定グループ
        for (TaskGroup group : scheduleGroups) {

            int completedCount =
                0;

            for (Task task : group.getTasks()) {

                if (task.isCompleted()) {

                    completedCount++;
                }
            }

            String groupStatus;

            if (completedCount == 0) {

                groupStatus =
                    "INCOMPLETE";

            } else if (
                completedCount
                == group.getTasks().size()
            ) {

                groupStatus =
                    "COMPLETED";

            } else {

                groupStatus =
                    "PARTIAL";
            }

            // グループ内ステータス設定処理
            for (Task task : group.getTasks()) {

                groupStatuses.put(
                    task.getId(),
                    groupStatus
                );
            }
        }

        // 締切グループ
        for (TaskGroup group : deadlineGroups) {

            int completedCount =
                0;

            for (Task task : group.getTasks()) {

                if (task.isCompleted()) {

                    completedCount++;
                }
            }

            String groupStatus;

            if (completedCount == 0) {

                groupStatus =
                    "INCOMPLETE";

            } else if (
                completedCount
                == group.getTasks().size()
            ) {

                groupStatus =
                    "COMPLETED";

            } else {

                groupStatus =
                    "PARTIAL";
            }

            // グループ内ステータス設定処理
            for (Task task : group.getTasks()) {

                groupStatuses.put(
                    task.getId(),
                    groupStatus
                );
            }
        }

        // タスク一覧グループのステータス判定
        for (TaskGroup group : visibleTaskGroups) {

            int completedCount =
                0;

            for (Task task : group.getTasks()) {

                if (task.isCompleted()) {

                    completedCount++;
                }
            }

            String groupStatus;

            if (completedCount == 0) {

                groupStatus =
                    "INCOMPLETE";

            } else if (
                completedCount
                == group.getTasks().size()
            ) {

                groupStatus =
                    "COMPLETED";

            } else {

                groupStatus =
                    "PARTIAL";
            }

            // タスク一覧グループ内ステータス設定処理
            for (Task task : group.getTasks()) {

                taskListGroupStatuses.put(
                    task.getId(),
                    groupStatus
                );
            }
        }

        // Model
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
            "groupStatuses",
            groupStatuses
        );

        model.addAttribute(
            "taskListGroupStatuses",
            taskListGroupStatuses
        );

        model.addAttribute(
            "today",
            today
        );

        model.addAttribute(
            "threeDaysLater",
            today.plusDays(3)
        );

        //ログインユーザー情報
        model.addAttribute(
            "user",
            loginUser
        );

        // ログインユーザー名表示処理
        model.addAttribute(
            "username",
            loginUser.getUsername()
        );

        // 管理者判定処理
        boolean isAdmin =
            "ADMIN".equals(
                loginUser.getRole()
            );

        model.addAttribute(
            "isAdmin",
            isAdmin
        );

        //お問い合わせ未読返信件数取得処理
        long unreadReplyCount =
            contactRepository
                .countByUsernameAndReplyReadFalseAndReplyIsNotNull(
                    loginUser.getUsername()
                );

        model.addAttribute(
            "unreadReplyCount",
            unreadReplyCount
        );

        //新しい返信通知判定
        model.addAttribute(
            "newReply",
            Boolean.TRUE.equals(newReply)
        );

        return "task/index";
    }

    // 履歴画面処理
    @GetMapping("/history")
    public String history(
        @RequestParam(required = false) Integer year,
        @RequestParam(required = false) Integer month,
        @RequestParam(required = false) Integer day,
        @RequestParam(required = false) String dateType,
        @RequestParam(defaultValue = "0") int page,
        Model model,
        Principal principal
    ) {

        // ログインユーザー取得処理
        User loginUser =
            userRepository
                .findByUsername(
                    principal.getName()
                )
                .orElseThrow();

        // ログインユーザーのタスク取得処理
        List<Task> tasks =
            taskRepository.findByUser(
                loginUser
            );

        // 全タスクグループ作成処理
        ArrayList<TaskGroup> allGroups =
            new ArrayList<>();

        for (Task task : tasks) {

            TaskGroup foundGroup =
                null;

            for (TaskGroup group : allGroups) {

                boolean sameDate =
                    group.getDeadline()
                        .equals(
                            task.getDeadline()
                        );

                boolean sameTitle =
                    group.getTitle()
                        .equals(
                            task.getTitle()
                        );

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

                    foundGroup =
                        group;

                    break;
                }
            }

            if (foundGroup != null) {

                foundGroup.addTask(
                    task
                );

            } else {

                TaskGroup newGroup =
                    new TaskGroup(
                        task.getDeadline(),
                        task.getTitle()
                    );

                newGroup.addTask(
                    task
                );

                allGroups.add(
                    newGroup
                );
            }
        }

        // 全件完了グループ抽出処理
        ArrayList<TaskGroup> completedGroups =
            new ArrayList<>();

        for (TaskGroup group : allGroups) {

            boolean allCompleted =
                true;

            for (Task task : group.getTasks()) {

                if (!task.isCompleted()) {

                    allCompleted =
                        false;

                    break;
                }
            }

            if (allCompleted) {

                completedGroups.add(
                    group
                );
            }
        }

        // 履歴絞り込み処理
        ArrayList<TaskGroup> filteredHistoryGroups =
            new ArrayList<>();

        for (TaskGroup group : completedGroups) {

            LocalDate deadline =
                group.getDeadline();

            boolean matchesYear =
                year == null
                || deadline.getYear()
                == year;

            boolean matchesMonth =
                month == null
                || deadline.getMonthValue()
                == month;

            boolean matchesDay =
                day == null
                || deadline.getDayOfMonth()
                == day;

            boolean matchesDateType =
                dateType == null
                || dateType.isBlank()
                || "ALL".equals(dateType)
                || group.getTasks()
                    .get(0)
                    .getDateType()
                    .name()
                    .equals(dateType);

            if (
                matchesYear
                && matchesMonth
                && matchesDay
                && matchesDateType
            ) {

                filteredHistoryGroups.add(
                    group
                );
            }
        }

        // 履歴日付降順処理
        filteredHistoryGroups.sort(
            Comparator
                .comparing(
                    TaskGroup::getDeadline
                )
                .reversed()
        );

        // 完了日時計算処理
        Map<Long, LocalDateTime> groupCompletedAt =
            new HashMap<>();

        for (TaskGroup group : filteredHistoryGroups) {

            LocalDateTime latestCompletedAt =
                null;

            for (Task task : group.getTasks()) {

                LocalDateTime completedAt =
                    task.getCompletedAt();

                if (
                    completedAt != null
                    && (
                        latestCompletedAt == null
                        || completedAt.isAfter(
                            latestCompletedAt
                        )
                    )
                ) {

                    latestCompletedAt =
                        completedAt;
                }
            }

            groupCompletedAt.put(
                group.getTasks()
                    .get(0)
                    .getId(),
                latestCompletedAt
            );
        }

        // 履歴年選択肢作成処理
        ArrayList<Integer> historyYears =
            new ArrayList<>();

        for (TaskGroup group : completedGroups) {

            int groupYear =
                group.getDeadline()
                    .getYear();

            if (
                !historyYears.contains(
                    groupYear
                )
            ) {

                historyYears.add(
                    groupYear
                );
            }
        }

        historyYears.sort(
            Comparator.reverseOrder()
        );

        // ページネーション処理
        int pageSize =
            10;

        int totalGroups =
            filteredHistoryGroups.size();

        int totalPages =
            (
                totalGroups
                + pageSize
                - 1
            )
            / pageSize;

        if (page < 0) {

            page =
                0;
        }

        if (
            totalPages > 0
            && page >= totalPages
        ) {

            page =
                totalPages - 1;
        }

        int startIndex =
            page
            * pageSize;

        int endIndex =
            Math.min(
                startIndex + pageSize,
                totalGroups
            );

        ArrayList<TaskGroup> pagedHistoryGroups =
            new ArrayList<>();

        if (
            startIndex < totalGroups
        ) {

            pagedHistoryGroups.addAll(
                filteredHistoryGroups
                    .subList(
                        startIndex,
                        endIndex
                    )
            );
        }

        // 履歴日付グループ作成処理
        ArrayList<TaskDateGroup> historyDateGroups =
            new ArrayList<>();

        for (TaskGroup group : pagedHistoryGroups) {

            TaskDateGroup foundDateGroup =
                null;

            for (
                TaskDateGroup dateGroup :
                historyDateGroups
            ) {

                if (
                    dateGroup.getDeadline()
                        .equals(
                            group.getDeadline()
                        )
                ) {

                    foundDateGroup =
                        dateGroup;

                    break;
                }
            }

            if (foundDateGroup != null) {

                foundDateGroup.addGroup(
                    group
                );

            } else {

                TaskDateGroup newDateGroup =
                    new TaskDateGroup(
                        group.getDeadline()
                    );

                newDateGroup.addGroup(
                    group
                );

                historyDateGroups.add(
                    newDateGroup
                );
            }
        }

        // 履歴画面表示データ設定処理
        model.addAttribute(
            "historyDateGroups",
            historyDateGroups
        );

        model.addAttribute(
            "groupCompletedAt",
            groupCompletedAt
        );

        model.addAttribute(
            "historyYears",
            historyYears
        );

        model.addAttribute(
            "selectedYear",
            year
        );

        model.addAttribute(
            "selectedMonth",
            month
        );

        model.addAttribute(
            "selectedDay",
            day
        );

        model.addAttribute(
            "selectedDateType",
            dateType
        );

        model.addAttribute(
            "currentPage",
            page
        );

        model.addAttribute(
            "totalPages",
            totalPages
        );

        model.addAttribute(
            "totalHistoryCount",
            totalGroups
        );

        return "task/history";
    }

    // タスク追加画面
    @GetMapping("/add")
    public String addTask(
        @RequestParam(required = false) LocalDate deadline,
        @RequestParam(required = false) DateType dateType,
        Model model
    ){
        //日付指定で追加画面を開いた場合
        model.addAttribute("selectedDeadline",deadline);

        //予定・締切区分指定
        model.addAttribute("selectedDateType",dateType);

        return "task/addTask";
    }
    
    


    // タスク追加処理
    @PostMapping("/add")
    public String addTask(
        @RequestParam String title,
        @RequestParam LocalDate deadline,
        @RequestParam(required = false) LocalTime time,
        @RequestParam(required = false) LocalTime endTime,
        @RequestParam DateType dateType,
        @RequestParam(required = false) String description,
        Principal principal,
        RedirectAttributes redirectAttributes
    ) {

        // ログインユーザー取得処理
        User loginUser =
            userRepository
                .findByUsername(
                    principal.getName()
                )
                .orElseThrow();

        //締切の場合は終了時間を使用しない
        if (dateType == DateType.DEADLINE) {

            endTime =
                null;
        }

        //終了時間のみ入力チェック
        if (
            time == null
            && endTime != null
        ) {

            redirectAttributes.addFlashAttribute(
                "timeError",
                "開始時間を入力してください。"
            );

            redirectAttributes.addFlashAttribute(
                "enteredTitle",
                title
            );

            redirectAttributes.addFlashAttribute(
                "enteredDeadline",
                deadline
            );

            redirectAttributes.addFlashAttribute(
                "enteredTime",
                time
            );

            redirectAttributes.addFlashAttribute(
                "enteredEndTime",
                endTime
            );

            redirectAttributes.addFlashAttribute(
                "enteredDateType",
                dateType
            );

            redirectAttributes.addFlashAttribute(
                "enteredDescription",
                description
            );

            return "redirect:/add";
        }

        //終了時間チェック
        if (
            time != null
            && endTime != null
            && (
                endTime.isBefore(time)
                || endTime.equals(time)
            )
        ) {

            redirectAttributes.addFlashAttribute(
                "timeError",
                "終了時間は開始時間より後にしてください。"
            );

            redirectAttributes.addFlashAttribute(
                "enteredTitle",
                title
            );

            redirectAttributes.addFlashAttribute(
                "enteredDeadline",
                deadline
            );

            redirectAttributes.addFlashAttribute(
                "enteredTime",
                time
            );

            redirectAttributes.addFlashAttribute(
                "enteredEndTime",
                endTime
            );

            redirectAttributes.addFlashAttribute(
                "enteredDateType",
                dateType
            );

            redirectAttributes.addFlashAttribute(
                "enteredDescription",
                description
            );

            return "redirect:/add";
        }

        Task task =
            new Task(
                title,
                deadline,
                time,
                endTime,
                dateType,
                description
            );

        //タスク全体の時間設定
        task.setGroupStartTime(time);

        //予定の場合
        if(dateType == DateType.SCHEDULE){
            task.setGroupEndTime(endTime);
        //締切の場合
        }else{
            task.setGroupEndTime(null);
        }

        //個別タスクの時間は未設定
        task.setTime(null);
        task.setEndTime(null);

        // タスク所有ユーザー設定
        task.setUser(
            loginUser
        );

        

        taskRepository.save(
            task
        );

        return "redirect:/";
    }

// 既存タスクに時間・詳細を追加する画面
    @GetMapping("/task/time/add/{id}")
    public String addTaskTime(
        @PathVariable Long id,
        Model model,
        Principal principal
    ) {

        // ログインユーザー取得処理
        User loginUser =
            userRepository
                .findByUsername(
                    principal.getName()
                )
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
                .equals(
                    loginUser.getId()
                )
        ) {

            return "redirect:/";
        }

        model.addAttribute(
            "task",
            task
        );

        return "task/addScheduleTime";
    }

    // 既存タスクに時間・詳細を追加する処理
    @PostMapping("/task/time/add/{id}")
    public String addTaskTime(
        @PathVariable Long id,
        @RequestParam(required = false) LocalTime time,
        @RequestParam(required = false) LocalTime endTime,
        @RequestParam(required = false) String description,
        Principal principal,
        RedirectAttributes redirectAttributes
    ) {

    // ログインユーザー取得処理
    User loginUser =
        userRepository
            .findByUsername(
                principal.getName()
            )
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
            .equals(
                loginUser.getId()
            )
    ) {

        return "redirect:/";
    }

    //締切の場合は終了時間を使用しない
    if (
        baseTask.getDateType()
        == DateType.DEADLINE
    ) {

        endTime =
            null;
    }

    //終了時間のみ入力チェック
    if (
        time == null
        && endTime != null
    ) {

        redirectAttributes.addFlashAttribute(
            "taskTimeAddError",
            "開始時間を入力してください。"
        );

        return "redirect:/task/time/add/" + id;
    }

    //終了時間チェック
    if (
        time != null
        && endTime != null
        && (
            endTime.isBefore(time)
            || endTime.equals(time)
        )
    ) {

        redirectAttributes.addFlashAttribute(
            "taskTimeAddError",
            "終了時間は開始時間より後にしてください。"
        );

        return "redirect:/task/time/add/" + id;
    }

    //予定全体の時間範囲チェック
    if (
        baseTask.getDateType() == DateType.SCHEDULE
        && time != null   
    ) {
        LocalTime groupStartTime = baseTask.getGroupStartTime();
        LocalTime groupEndTime = baseTask.getGroupEndTime();

        //開始時間が予定全体より前
        if(groupStartTime != null && time.isBefore(groupStartTime)){
            redirectAttributes.addFlashAttribute("taskTimeAddError","開始時間は予定全体の開始時間以降にしてください。");

            return "redirect:/task/time/add/" + id;
        }

        //開始時間が予定全体より後
        if(
            groupEndTime != null
            && time.isAfter(groupEndTime)
        ){
            redirectAttributes.addFlashAttribute("taskTimeAddError","開始時間は予定全体の終了以前にしてください。");

            return "redirect:/task/time/add/" + id;
        }

        //終了時間が予定全体より後
        if(
            groupEndTime != null
            && endTime != null
            && endTime.isAfter(groupEndTime)
        ){
            redirectAttributes.addFlashAttribute("taskTimeAddError","終了時間は予定全体の終了時間以前にしてください。");

            return "redirect:/task/time/add/" + id;   
        }
        
    }

    //締切時間の範囲チェック
    if(
        baseTask.getDateType() == DateType.DEADLINE && time != null
    ){
        LocalTime deadlineTime = baseTask.getGroupStartTime();

        //締切より後
        if (
            deadlineTime != null
            && time.isAfter(deadlineTime)
        ) {
            redirectAttributes.addFlashAttribute("taskTimeAddError", "時間は締切時間以前にしてください。");

            return "redirect:/task/time/add/" + id;
        }
    }

    // 時間・詳細未入力チェック
    if (
        time == null
        && (
            description == null
            || description.isBlank()
        )
    ) {

        redirectAttributes.addFlashAttribute(
            "taskTimeAddError",
            "時間または詳細のどちらかを入力してください。"
        );

        return "redirect:/task/time/add/" + id;
    }

    Task newTask =
        new Task(
            baseTask.getTitle(),
            baseTask.getDeadline(),
            time,
            endTime,
            baseTask.getDateType(),
            description
        );

    //予定全体の時間を引き継ぐ
    newTask.setGroupStartTime(baseTask.getGroupStartTime());

    newTask.setGroupEndTime(baseTask.getGroupEndTime());

    // タスク所有ユーザー設定
    newTask.setUser(
        loginUser
    );

    taskRepository.save(
        newTask
    );

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
                .findByUsername(
                    principal.getName()
                )
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
                .equals(
                    loginUser.getId()
                )
        ) {

            return "redirect:/";
        }

        task.complete();

        taskRepository.save(
            task
        );

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
                .findByUsername(
                    principal.getName()
                )
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
                .equals(
                    loginUser.getId()
                )
        ) {

            return "redirect:/";
        }

        task.incomplete();

        taskRepository.save(
            task
        );

        return "redirect:/";
    }

    // 履歴から個別タスクを未完了に戻す処理
    @GetMapping("/history/incomplete/{id}")
    public String historyIncompleteTask(
        @PathVariable Long id,
        Principal principal
    ) {
        // ログインユーザー取得処理
        User loginUser =
            userRepository
                .findByUsername(
                    principal.getName()
                )
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
                .equals(
                    loginUser.getId()
                )
        ) {

            return "redirect:/history";
        }

        task.incomplete();

        taskRepository.save(
            task
        );

        return "redirect:/history";
    }

    // 履歴からグループを未完了に戻す処理
    @GetMapping("/history/incomplete/group/{id}")
    public String historyIncompleteTaskGroup(
        @PathVariable Long id,
        Principal principal
    ) {

        // ログインユーザー取得処理
        User loginUser =
            userRepository
                .findByUsername(
                    principal.getName()
                )
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
                .equals(
                    loginUser.getId()
                )
            ) {

            return "redirect:/history";
        }

        // ログインユーザーのタスク取得処理
        List<Task> tasks =
            taskRepository.findByUser(
                loginUser
            );

        for (Task task : tasks) {
            boolean sameDate =
                task.getDeadline()
                    .equals(
                        baseTask.getDeadline()
                    );

            boolean sameTitle =
                task.getTitle()
                    .equals(
                        baseTask.getTitle()
                    );

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
        return "redirect:/history";
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
                .findByUsername(
                    principal.getName()
                )
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
                .equals(
                    loginUser.getId()
                )
        ) {

            return "redirect:/";
        }

        // ログインユーザーのタスク取得処理
        List<Task> tasks =
            taskRepository.findByUser(
                loginUser
            );

        for (Task task : tasks) {

            boolean sameDate =
                task.getDeadline()
                    .equals(
                        baseTask.getDeadline()
                    );

            boolean sameTitle =
                task.getTitle()
                    .equals(
                        baseTask.getTitle()
                    );

            boolean sameDateType =
                task.getDateType()
                == baseTask.getDateType();

            if (
                sameDate
                && sameTitle
                && sameDateType
            ) {

                task.complete();

                taskRepository.save(
                    task
                );
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
                .findByUsername(
                    principal.getName()
                )
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
                .equals(
                    loginUser.getId()
                )
        ) {

            return "redirect:/";
        }

        // ログインユーザーのタスク取得処理
        List<Task> tasks =
            taskRepository.findByUser(
                loginUser
            );

        for (Task task : tasks) {

            boolean sameDate =
                task.getDeadline()
                    .equals(
                        baseTask.getDeadline()
                    );

            boolean sameTitle =
                task.getTitle()
                    .equals(
                        baseTask.getTitle()
                    );

            boolean sameDateType =
                task.getDateType()
                == baseTask.getDateType();

            if (
                sameDate
                && sameTitle
                && sameDateType
            ) {

                task.incomplete();

                taskRepository.save(
                    task
                );
            }
        }

        return "redirect:/";
    }

    // タスク全体編集画面
    @GetMapping("/edit/group/{id}")
    public String editTaskGroup(
        @PathVariable Long id,
        Model model,
        Principal principal
    ) {

        // ログインユーザー取得処理
        User loginUser =
            userRepository
                .findByUsername(
                    principal.getName()
                )
                .orElseThrow();

        // 基準タスク取得処理
        Task baseTask =
            taskRepository
                .findById(id)
                .orElseThrow();

        // タスク所有ユーザー確認処理
        if (
            baseTask.getUser() == null
            || !baseTask.getUser()
                .getId()
                .equals(
                    loginUser.getId()
                )
        ) {

            return "redirect:/";
        }

        // タスク全体編集画面表示用データ設定
        model.addAttribute(
            "task",
            baseTask
        );

        return "task/editTaskGroup";
    }

    // タスク全体更新処理
    @PostMapping("/edit/group/{id}")
    public String updateTaskGroup(
        @PathVariable Long id,
        @RequestParam String title,
        @RequestParam LocalDate deadline,
        @RequestParam DateType dateType,
        @RequestParam(required = false)LocalTime groupStartTime,
        @RequestParam(required = false)LocalTime groupEndTime,
        Principal principal,
        RedirectAttributes redirectAttributes
    ) {

        // ログインユーザー取得処理
        User loginUser =
            userRepository
                .findByUsername(
                    principal.getName()
                )
                .orElseThrow();

        // 基準タスク取得処理
        Task baseTask =
            taskRepository
                .findById(id)
                .orElseThrow();

        // タスク所有ユーザー確認処理
        if (
            baseTask.getUser() == null
            || !baseTask.getUser()
                .getId()
                .equals(
                    loginUser.getId()
                )
        ) {

            return "redirect:/";
        }

        //締切の場合は予定全体の時間を使用しない
        if(dateType == DateType.DEADLINE){
            groupEndTime = null;
        }

        //終了時間のみ入力チェック
        if(
            groupStartTime == null
            && groupEndTime != null
        ){
            redirectAttributes.addFlashAttribute("timeError","開始時間を入力してください。");

            return "redirect:/edit/group/" + id;
        }

        //終了時間チェック
        if (
             groupStartTime != null
             && groupEndTime != null
             &&(
                groupEndTime.isBefore(groupStartTime)
                || groupEndTime.equals(groupStartTime)
             )
        ) {
            redirectAttributes.addFlashAttribute("timeError","終了時間は開始時間より後にしてください。");

            return "redirect:/edit/group/" + id;
        }

        // 変更前グループ情報保持処理
        String oldTitle =
            baseTask.getTitle();

        LocalDate oldDeadline =
            baseTask.getDeadline();

        DateType oldDateType =
            baseTask.getDateType();

        // ログインユーザーのタスク取得処理
        List<Task> tasks =
            taskRepository.findByUser(
                loginUser
            );

        //既存タスクの時間範囲チェック
        if(dateType == DateType.SCHEDULE){
            for(Task task : tasks){
                boolean sameDate = task.getDeadline().equals(oldDeadline);
                boolean sameTitle = task.getTitle().equals(oldTitle);
                boolean sameDateType = task.getDateType() == oldDateType;

                if(sameDate && sameTitle && sameDateType){
                    //タスク開始時間が予定全体より前
                    if(
                        groupStartTime != null
                        && task.getTime() != null
                        && task.getTime().isBefore(groupStartTime)
                    ){
                        redirectAttributes.addFlashAttribute("timeError","既存のタスク時間が予定全体の時間範囲外になります。");

                        return "redirect:/edit/group/" + id;
                    }

                    //タスク開始時間が予定全体より後
                    if(
                        groupEndTime != null
                        && task.getTime() != null
                        && task.getTime().isAfter(groupEndTime)
                    ){
                        redirectAttributes.addFlashAttribute("timeError","既存のタスク時間が予定全体の時間範囲外になります。");

                        return "redirect:/edit/group/" + id;
                    }

                    //タスク終了時間が予定全体より後
                    if(
                        groupEndTime != null
                        && task.getEndTime() != null
                        && task.getEndTime().isAfter(groupEndTime)
                    ){
                        redirectAttributes.addFlashAttribute("timeError","既存タスク時間が予定全体の時間範囲外になります。");

                        return "redirect:/edit/group/" + id;
                    }
                }
            }
        }

        //締切時間の範囲チェック
        if(dateType == DateType.DEADLINE){
            for(Task task : tasks){
                boolean sameDate = task.getDeadline().equals(oldDeadline);
                boolean sameTitle = task.getTitle().equals(oldTitle);
                boolean sameDateType = task.getDateType() == oldDateType;

                if(sameDate && sameTitle && sameDateType){

                    //既存タスクが締切時間より後
                    if(
                        groupStartTime != null
                        && task.getTime() != null
                        && task.getTime().isAfter(groupStartTime)
                    ){
                        redirectAttributes.addFlashAttribute(
                            "timeError",
                            "既存のタスク時間が締切時間より後になります。"
                        );

                        return "redirect:/edit/group/" + id;
                    }
                }
            }
        }

        // 同一グループ一括更新処理
        for (Task task : tasks) {

            boolean sameDate =
                task.getDeadline()
                    .equals(
                        oldDeadline
                    );

            boolean sameTitle =
                task.getTitle()
                    .equals(
                        oldTitle
                    );

            boolean sameDateType =
                task.getDateType()
                == oldDateType;

            if (
                sameDate
                && sameTitle
                && sameDateType
            ) {

                task.setTitle(
                    title
                );

                task.setDeadline(
                    deadline
                );

                task.setDateType(
                    dateType
                );

                task.setGroupStartTime(
                    groupStartTime
                );

                task.setGroupEndTime(
                    groupEndTime
                );

                taskRepository.save(
                    task
                );
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
                .findByUsername(
                    principal.getName()
                )
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
                .equals(
                    loginUser.getId()
                )
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
        @RequestParam(required = false) LocalTime time,
        @RequestParam(required = false) LocalTime endTime,
        @RequestParam DateType dateType,
        @RequestParam(required = false) String description,
        Principal principal,
        RedirectAttributes redirectAttributes
    ) {

        // ログインユーザー取得処理
        User loginUser =
            userRepository
                .findByUsername(
                    principal.getName()
                )
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
                .equals(
                    loginUser.getId()
                )
        ) {

            return "redirect:/";
        }

        //締切の場合は終了時間を使用しない
        if(dateType == DateType.DEADLINE){
            endTime = null;
        }

        //終了時間のみ入力チェック
        if(time == null && endTime != null){
            redirectAttributes.addFlashAttribute("timeError","開始時間を入力してください");

            return "redirect:/edit/" + id;
        }

        //終了時間チェック
        if(time != null && endTime != null
            &&(endTime.isBefore(time) || endTime.equals(time))        
        ){
            redirectAttributes.addFlashAttribute("timeError", "終了時間は開始時間より後にしてください。");

            return "redirect:/edit/" + id;
        }

        //予定全体の時間範囲チェック
        if(
            task.getDateType() == DateType.SCHEDULE
            && time != null
        ){

            LocalTime groupStartTime = task.getGroupStartTime();
            LocalTime groupEndTime = task.getGroupEndTime();

            //開始時間が予定全体より前
            if(
                groupStartTime != null
                && time.isBefore(groupStartTime)
            ){
                redirectAttributes.addFlashAttribute("timeError","開始時間は予定全体の開始時間以降にしてください。");

                return "redirect:/edit/" + id;
            }

            //開始時間が予定全体より後
            if(
                groupEndTime != null
                && time.isAfter(groupEndTime)
            ){
                redirectAttributes.addFlashAttribute("timeError","開始時間は予定全体の終了時間以前にしてください。"
                );

                return "redirect:/edit/" + id;
            }

            //終了時間が予定全体より後
            if(
                groupEndTime != null
                && endTime != null
                && endTime.isAfter(groupEndTime)
            ){
                redirectAttributes.addFlashAttribute("timeError","終了時間は予定全体の終了時間以前にしてください。");

                return "redirect:/edit/" + id;
            }
        }
        //締切時間の範囲チェック
        if(
            task.getDateType() == DateType.DEADLINE
            && time != null
        ){
            LocalTime deadlineTime = task.getGroupStartTime();

            //締切時間より後
            if(deadlineTime != null && time.isAfter(deadlineTime)){
                redirectAttributes.addFlashAttribute("timeError", "時間は締切時間以前にしてください。");

                return "redirect:/edit/" + id;
            }
        }

        task.setTitle(
            title
        );

        task.setDeadline(
            deadline
        );

        task.setTime(
            time
        );

        task.setEndTime(
            endTime
        );

        task.setDateType(
            dateType
        );

        task.setDescription(
            description
        );

        taskRepository.save(task);

        return "redirect:/";
    }

    //タスク全体削除処理
    @GetMapping("/delete/group/{id}")
    public String deleteTaskGroup(
        @PathVariable Long id,
        Principal principal
    ) {

        //ログインユーザー取得
        User loginUser =
            userRepository
                .findByUsername(
                    principal.getName()
                )
                .orElseThrow();

        //基準タスク取得処理
        Task baseTask =
            taskRepository
                .findById(id)
                .orElseThrow();

        //タスク所有ユーザー確認処理
        if (
            baseTask.getUser() == null
            || !baseTask.getUser()
                .getId()
                .equals(
                    loginUser.getId()
                )
        ) {

            return "redirect:/";
        }

        //ログインユーザーのタスク取得処理
        List<Task> tasks =
            taskRepository.findByUser(
                loginUser
            );

        //同一グループ削除処理
        for (Task task : tasks) {

            boolean sameDate =
                task.getDeadline()
                    .equals(
                        baseTask.getDeadline()
                    );

            boolean sameTitle =
                task.getTitle()
                    .equals(
                        baseTask.getTitle()
                    );

            boolean sameDateType =
                task.getDateType()
                == baseTask.getDateType();

            if (
                sameDate
                && sameTitle
                && sameDateType
            ) {

                taskRepository.delete(
                    task
                );
            }
        }

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
                .findByUsername(
                    principal.getName()
                )
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
                .equals(
                    loginUser.getId()
                )
        ) {

            return "redirect:/";
        }

        taskRepository.deleteById(
            id
        );

        return "redirect:/";
    }
}