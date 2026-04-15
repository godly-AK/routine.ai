package com.example.routineai;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private JsonArray chatHistory = new JsonArray();
    private static final String API_KEY = BuildConfig.GEMINI_API_KEY; // Move this to BuildConfig

    private LinearLayout chatContainer;
    private ScrollView scrollView;

    // Shared executor — not recreated on every DB save
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        // Ask for Notification Permission on modern Android versions
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        chatContainer = findViewById(R.id.chatContainer);
        scrollView = findViewById(R.id.scrollView);
        EditText editTextText = findViewById(R.id.editTextText);
        ImageButton button = findViewById(R.id.button);

        // Restore chat history across rotations
        if (savedInstanceState != null) {
            String saved = savedInstanceState.getString("chat_history");
            if (saved != null) {
                chatHistory = com.google.gson.JsonParser.parseString(saved).getAsJsonArray();
            }
        }

        // Add system prompt only once — when history is truly empty
        if (chatHistory.isEmpty()) {
            String systemPrompt = "You are an AI discipline coach. You must ALWAYS respond in valid JSON format. " +
                    "1. If brainstorming/advice, use: {\"mode\": \"chat\", \"message\": \"...\"}. " +
                    "2. If scheduling/updating, use: {\"mode\": \"schedule\", \"message\": \"...\", \"tasks\": [...]}. " +
                    "RULES: ALWAYS return the FULL updated schedule; Use 24h format; " +
                    "CRITICAL CATEGORY RULE: You may ONLY choose from: [\"academics\", \"gaming\", \"fitness\", \"chores\", \"other\"]. " +
                    "Each task MUST include: title, scheduled_time, priority, duration_minutes, categories, and status. " +
                    "CRITICAL STATUS RULE: If the task is a one-time event, status MUST be exactly 'Pending'. " +
                    "If it is a daily repeating habit, status MUST be exactly 'Recurring'. " +
                    "Output ONLY a raw JSON object starting with '{' and ending with '}'. No markdown, no code fences.";
            addToHistory("user", systemPrompt);

            // Paired model reply so Gemini gets alternating user/model turns
            addToHistory("model", "Understood. I will always respond in valid JSON format.");
        }

        button.setOnClickListener(v -> {
            String userInput = editTextText.getText().toString().trim();
            if (userInput.isEmpty()) return;
            // 🌟 THE DYNAMIC SQL CONSOLE (SUDO MODE) - MULTI-LINE UPGRADE 🌟
            if (userInput.toUpperCase().startsWith("SUDO ")) {
                String rawQuery = userInput.substring(5).trim();
                addChatBubble("Executing Raw SQL:\n" + rawQuery, true);
                editTextText.setText(""); // Make sure this matches your EditText ID!

                executor.execute(() -> {
                    AppDatabase db = AppDatabase.getDatabase(MainActivity.this);
                    androidx.sqlite.db.SupportSQLiteDatabase sqlDb = db.getOpenHelper().getWritableDatabase();

                    try {
                        // Start a transaction so if one line fails, it rolls everything back safely
                        sqlDb.beginTransaction();

                        // Split the block of text by semicolons
                        String[] queries = rawQuery.split(";");

                        for (String query : queries) {
                            if (!query.trim().isEmpty()) {
                                sqlDb.execSQL(query.trim()); // Execute each line one by one
                            }
                        }

                        sqlDb.setTransactionSuccessful(); // Mark all as successful

                        runOnUiThread(() -> {
                            addChatBubble("✅ All queries executed successfully.", false);
                        });

                    } catch (Exception e) {
                        runOnUiThread(() -> {
                            addChatBubble("❌ SQL Error on line:\n" + e.getMessage(), false);
                        });
                    } finally {
                        sqlDb.endTransaction(); // Close the transaction
                    }
                });

                return; // EXIT EARLY!
            }

            // 1. Show the user's text in the UI immediately
            addChatBubble( userInput,true);
            editTextText.setText("");

            // 2. Run a background thread to fetch current tasks from the database
            executor.execute(() -> {
                AppDatabase db = AppDatabase.getDatabase(this);

                List<Task> activeTasks = db.taskDao().getActiveTasksForPrompt();

                StringBuilder existingTasksText = new StringBuilder();
                if (!activeTasks.isEmpty()) {
                    existingTasksText.append("\n\nIMPORTANT: The following tasks already exist in the database. ");
                    existingTasksText.append("You MUST include them in the new schedule with EVERY field exactly as shown — ");
                    existingTasksText.append("do not rename, reschedule, or change any value:\n\n");
                    existingTasksText.append("only DELETE by not putting in JSON unless specifically mentioned in the prompt.");

                    for (Task t : activeTasks) {
                        existingTasksText.append("{\n")
                                .append("  routine_id: ").append(t.routine_id).append(",\n")
                                .append("  title: \"").append(t.title).append("\",\n")
                                .append("  scheduled_time: \"").append(t.scheduledTime).append("\",\n")
                                .append("  duration_minutes: ").append(t.durationMinutes).append(",\n")
                                .append("  priority: \"").append(t.priority).append("\",\n")
                                .append("  status: \"").append(t.status).append("\"\n")
                                .append("},\n");
                    }

                    existingTasksText.append("\nOnly add NEW tasks around the above. Do not modify existing ones.");
                }

                String finalPrompt = userInput + existingTasksText.toString();



                // 3. Send the hidden, combined prompt to Gemini on the main thread
                runOnUiThread(() -> {
                    addChatBubble("Thinking…", false);
                    sendToGemini(finalPrompt.toString());
                });

            });
        });
    }

    // Save chat history so it survives rotation
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("chat_history", chatHistory.toString());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }

    // --- Helper to cleanly add a message to chatHistory ---
    private void addToHistory(String role, String text) {
        JsonObject obj = new JsonObject();
        obj.addProperty("role", role);
        JsonArray parts = new JsonArray();
        JsonObject part = new JsonObject();
        part.addProperty("text", text);
        parts.add(part);
        obj.add("parts", parts);
        chatHistory.add(obj);
    }
    // --- Helper to keep token usage low while preserving the System Prompt ---
    private void trimChatHistory() {
        // We want to keep:
        // Index 0 & 1: The System Prompt and AI's acknowledgment (Crucial for JSON)
        // The last 3 interactions (6 messages total: 3 user, 3 model)
        // Total max size = 8.

        // If it grows larger than 8, we delete the oldest User/Model pair (Index 2 and 3)
        // We call remove(2) twice because after the first removal, index 3 shifts down to index 2.
        while (chatHistory.size() > 8) {
            chatHistory.remove(2); // Removes the oldest User message
            chatHistory.remove(2); // Removes the oldest Model response
        }
    }

    // --- Chat bubble builder ---
    private void addChatBubble(String text, boolean isUser) {
        runOnUiThread(() -> {
            TextView bubble = new TextView(MainActivity.this);
            bubble.setText(text);
            bubble.setTextSize(16f);
            bubble.setPadding(40, 30, 40, 30);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 16, 0, 16);

            if (isUser) {
                bubble.setBackgroundResource(R.drawable.bg_user_bubble);
                bubble.setTextColor(android.graphics.Color.WHITE);
                params.gravity = android.view.Gravity.END;
            } else {
                bubble.setBackgroundResource(R.drawable.bg_ai_bubble);
                bubble.setTextColor(android.graphics.Color.BLACK);
                params.gravity = android.view.Gravity.START;
            }

            bubble.setLayoutParams(params);
            chatContainer.addView(bubble);
            scrollView.post(() -> scrollView.fullScroll(ScrollView.FOCUS_DOWN));
        });
    }

    private void sendToGemini(String promptText) {
        // Capture locally to prevent race condition with background thread
        final String capturedPrompt = promptText;

        // Add user message to history ONCE here — never again in onResponse
        addToHistory("user", capturedPrompt);
        trimChatHistory();

        JsonObject body = new JsonObject();
        body.add("contents", chatHistory);

        RetrofitClient.getApi().getRoutinePlan(API_KEY, body).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                // Remove "Thinking..." bubble
                runOnUiThread(() -> {
                    if (chatContainer.getChildCount() > 0) {
                        chatContainer.removeViewAt(chatContainer.getChildCount() - 1);
                    }
                });

                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String rawResponse = response.body()
                                .getAsJsonArray("candidates").get(0).getAsJsonObject()
                                .getAsJsonObject("content").getAsJsonArray("parts").get(0).getAsJsonObject()
                                .get("text").getAsString();

                        // Save AI reply to history
                        addToHistory("model", rawResponse);
                        rawResponse = rawResponse.replaceAll("```json", "").replaceAll("```", "").trim();

                        int firstBrace = rawResponse.indexOf("{");
                        int lastBrace = rawResponse.lastIndexOf("}");

                        if (firstBrace == -1 || lastBrace == -1) {
                            // AI replied but not in JSON format — show raw reply
                            addChatBubble(rawResponse, false);
                            return;
                        }

                        String cleanJson = rawResponse.substring(firstBrace, lastBrace + 1);
                        android.util.Log.d("GEMINI_RAW", cleanJson);

                        JsonObject aiDecision = com.google.gson.JsonParser.parseString(cleanJson).getAsJsonObject();
                        if (!aiDecision.has("mode") || !aiDecision.has("message")) {
                            android.util.Log.e("AI_ERROR", "Missing mode or message in: " + cleanJson);
                            addChatBubble("AI returned an unexpected format. Please try again.", false);
                            return;
                        }
                        String mode = aiDecision.get("mode").getAsString();
                        String aiMessage = aiDecision.get("message").getAsString();

                        if ("schedule".equals(mode)) {
                            JsonArray tasksArray = aiDecision.getAsJsonArray("tasks");
                            java.lang.reflect.Type listType =
                                    new com.google.gson.reflect.TypeToken<List<Task>>(){}.getType();
                            List<Task> newTasks = new com.google.gson.Gson().fromJson(tasksArray, listType);

                            executor.execute(() -> {
                                try {
                                    AppDatabase db = AppDatabase.getDatabase(MainActivity.this);
                                    TaskDao dao = db.taskDao();

                                    // Insert a new routine row for this prompt — never delete old ones
                                    DailyRoutine newRoutine = new DailyRoutine();
                                    newRoutine.raw_prompt = capturedPrompt;
                                    newRoutine.date_logged = new SimpleDateFormat(
                                            "yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
                                    long newRoutineId = dao.insertRoutine(newRoutine);

                                    List<Task> existingTasks = dao.getAllTasks();

                                    java.util.Map<String, Task> existingMap = new java.util.HashMap<>();
                                    for (Task t : existingTasks) {
                                        existingMap.put(t.title.toLowerCase().trim(), t);
                                    }

                                    java.util.Set<String> incomingTitles = new java.util.HashSet<>();
                                    for (Task t : newTasks) {
                                        incomingTitles.add(t.title.toLowerCase().trim());
                                    }

                                    // STEP 1 — Delete tasks the AI removed
                                    for (Task existing : existingTasks) {
                                        if ("Completed".equalsIgnoreCase(existing.status) ||
                                                "Missed".equalsIgnoreCase(existing.status)) {
                                            continue;
                                        }
                                        if (!incomingTitles.contains(existing.title.toLowerCase().trim())) {
                                            final int oldId = existing.id;
                                            runOnUiThread(() -> {
                                                android.app.AlarmManager am =
                                                        (android.app.AlarmManager) getSystemService(android.content.Context.ALARM_SERVICE);
                                                android.app.PendingIntent pi = android.app.PendingIntent.getBroadcast(
                                                        MainActivity.this, oldId,
                                                        new android.content.Intent(MainActivity.this, TaskAlarmReceiver.class),
                                                        android.app.PendingIntent.FLAG_NO_CREATE | android.app.PendingIntent.FLAG_IMMUTABLE);
                                                if (pi != null && am != null) { am.cancel(pi); pi.cancel(); }
                                            });
                                            dao.deleteMappingsByTask(existing.id);
                                            dao.deleteAlarmByTask(existing.id);
                                            dao.deleteRecurrenceByTask(existing.id);
                                            dao.deleteTask(existing);
                                        }
                                    }

                                    // STEP 2 — Update existing or insert new
                                    for (Task incoming : newTasks) {
                                        String key = incoming.title.toLowerCase().trim();
                                        Task existing = existingMap.get(key);

                                        if (existing != null) {
                                            // ── Task exists — update only changed fields ──
                                            boolean changed = false;

                                            if (incoming.scheduledTime != null &&
                                                    !incoming.scheduledTime.equals(existing.scheduledTime)) {
                                                existing.scheduledTime = incoming.scheduledTime;
                                                changed = true;
                                            }
                                            if (incoming.priority != null &&
                                                    !incoming.priority.equals(existing.priority)) {
                                                existing.priority = incoming.priority;
                                                changed = true;
                                            }
                                            if (incoming.durationMinutes != existing.durationMinutes) {
                                                existing.durationMinutes = incoming.durationMinutes;
                                                changed = true;
                                            }
                                            if (incoming.status != null &&
                                                    !incoming.status.equals(existing.status)) {
                                                existing.status = incoming.status;
                                                changed = true;
                                            }

                                            if (changed) {
                                                existing.routine_id = (int) newRoutineId;
                                                dao.updateTask(existing);
                                                final Task updated = existing;
                                                runOnUiThread(() -> scheduleTaskAlarm(updated));
                                            }

                                            // Sync Task_Recurrence if recurring status changed
                                            boolean incomingIsRecurring = "Recurring".equalsIgnoreCase(incoming.status);
                                            boolean hasRecurrenceRow = dao.getRecurrenceByTaskId(existing.id) != null;

                                            if (incomingIsRecurring && !hasRecurrenceRow) {
                                                TaskRecurrence recurrence = new TaskRecurrence();
                                                recurrence.task_id = existing.id;
                                                recurrence.frequency = "Daily";
                                                recurrence.counts_towards_score = true;
                                                dao.insertRecurrence(recurrence);
                                            } else if (!incomingIsRecurring && hasRecurrenceRow) {
                                                dao.deleteRecurrenceByTask(existing.id);
                                            }

                                        } else {
                                            // ── Brand-new task — insert everything ──
                                            incoming.routine_id = (int) newRoutineId;
                                            long newTaskId = dao.insertSingleTask(incoming);
                                            incoming.id = (int) newTaskId;

                                            // Insert recurrence row if recurring
                                            if ("Recurring".equalsIgnoreCase(incoming.status != null ? incoming.status.trim() : "")) {
                                                TaskRecurrence recurrence = new TaskRecurrence();
                                                recurrence.task_id = incoming.id;
                                                recurrence.frequency = "Daily";
                                                recurrence.counts_towards_score = true;
                                                dao.insertRecurrence(recurrence);
                                            }

                                            // Insert category mappings
                                            if (incoming.categories != null) {
                                                for (String categoryName : incoming.categories) {
                                                    String normalizedName = categoryName.trim().toLowerCase();
                                                    Long catId = dao.getCategoryIdByName(normalizedName);

                                                    if (catId == null) {
                                                        Category cat = new Category();
                                                        cat.category_name = normalizedName;
                                                        catId = dao.insertCategory(cat);
                                                    }

                                                    if (catId != null && catId != -1) {
                                                        TaskCategoryMapping mapping = new TaskCategoryMapping();
                                                        mapping.task_id = incoming.id;
                                                        mapping.category_id = catId.intValue();
                                                        dao.insertTaskCategoryMapping(mapping);
                                                    }
                                                }
                                            }

                                            // Insert alarm and schedule it
                                            if ("Pending".equalsIgnoreCase(incoming.status != null ? incoming.status.trim() : "") ||
                                                    "Recurring".equalsIgnoreCase(incoming.status != null ? incoming.status.trim() : "")) {
                                                Alarm dbAlarm = new Alarm();
                                                dbAlarm.task_id = incoming.id;
                                                dbAlarm.trigger_time = incoming.scheduledTime;
                                                dbAlarm.is_active = true;
                                                dao.insertAlarm(dbAlarm);

                                                final Task toSchedule = incoming;
                                                runOnUiThread(() -> scheduleTaskAlarm(toSchedule));
                                            }
                                        }
                                    }
                                } catch (Exception e) {
                                    android.util.Log.e("DB_ERROR", "Selective update failed", e);
                                    runOnUiThread(() -> addChatBubble(
                                            "⚠️ Failed to update schedule: " + e.getMessage(), false));
                                }
                            });

                            addChatBubble(aiMessage, false);

                        } else {
                            addChatBubble(aiMessage, false);
                        }

                    } catch (Exception e) {
                        addChatBubble("Parsing error — could you rephrase?", false);
                        android.util.Log.e("AI_ERROR", "Failed to parse AI response", e);
                    }

                } else {
                    // --- THIS WAS MISSING — now surfaces the real error ---
                    String errorBody = "";
                    try {
                        errorBody = response.errorBody() != null
                                ? response.errorBody().string()
                                : "empty error body";
                    } catch (Exception e) {
                        errorBody = "unreadable error body";
                    }
                    android.util.Log.e("GEMINI_ERROR", "HTTP " + response.code() + " | " + errorBody);
                    addChatBubble("API error " + response.code() + ". Check Logcat for details.", false);
                }
            }

            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                runOnUiThread(() -> {
                    if (chatContainer.getChildCount() > 0) {
                        chatContainer.removeViewAt(chatContainer.getChildCount() - 1);
                    }
                    addChatBubble("Network failure: " + t.getMessage(), false);
                });
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.top_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_stats) {
            // Launch the Productivity screen!
            startActivity(new Intent(MainActivity.this, ProductivityActivity.class));
            return true;
        }
        else if (item.getItemId() == R.id.action_view_schedule) {
            startActivity(new android.content.Intent(MainActivity.this, ScheduleActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void scheduleTaskAlarm(Task task) {
        try {
            String[] timeParts = task.scheduledTime.split(":");
            int hour = Integer.parseInt(timeParts[0]);
            int minute = Integer.parseInt(timeParts[1]);

            java.util.Calendar calendar = java.util.Calendar.getInstance();
            calendar.set(java.util.Calendar.HOUR_OF_DAY, hour);
            calendar.set(java.util.Calendar.MINUTE, minute);
            calendar.set(java.util.Calendar.SECOND, 0);

            if (calendar.before(java.util.Calendar.getInstance())) {
                calendar.add(java.util.Calendar.DATE, 1);
            }

            android.content.Intent intent = new android.content.Intent(this, TaskAlarmReceiver.class);
            intent.putExtra("TASK_TITLE", task.title);
            intent.putExtra("TASK_PRIORITY", task.priority);
            intent.putExtra("TASK_ID", task.id);
            intent.putExtra("IS_RECURRING", "Recurring".equalsIgnoreCase(task.status));

            android.app.PendingIntent pendingIntent = android.app.PendingIntent.getBroadcast(
                    this, task.id, intent,
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE
            );

            android.app.AlarmManager alarmManager =
                    (android.app.AlarmManager) getSystemService(android.content.Context.ALARM_SERVICE);

            if (alarmManager != null) {
                // ← This check was missing — crashes without it on Android 12+
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExact(android.app.AlarmManager.RTC_WAKEUP,
                                calendar.getTimeInMillis(), pendingIntent);
                    } else {
                        // Permission not granted — request it
                        android.content.Intent permIntent = new android.content.Intent(
                                android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                        startActivity(permIntent);
                    }
                } else {
                    alarmManager.setExact(android.app.AlarmManager.RTC_WAKEUP,
                            calendar.getTimeInMillis(), pendingIntent);
                }
            }
        } catch (Exception e) {
            android.util.Log.e("AlarmError", "Failed to schedule: " + task.title, e);
        }
    }
    private void runDailyResetIfNeeded() {
        android.content.SharedPreferences prefs =
                getSharedPreferences("routineai_prefs", MODE_PRIVATE);

        String lastReset = prefs.getString("last_reset_date", "");
        String today = new java.text.SimpleDateFormat(
                "yyyy-MM-dd", java.util.Locale.getDefault())
                .format(new java.util.Date());

        if (today.equals(lastReset)) return; // already reset today

        executor.execute(() -> {
            AppDatabase db = AppDatabase.getDatabase(this);
            TaskDao dao = db.taskDao();

            dao.deleteFinishedOneTimeTasks();   // removes Completed/Missed one-time rows
            dao.resetRecurringTasksForNewDay(); // resets Recurring tasks back to Pending

            // Save today so this doesn't run again until tomorrow
            prefs.edit().putString("last_reset_date", today).apply();

            android.util.Log.d("DAILY_RESET", "Daily reset ran for " + today);
        });
    }
    @Override
    protected void onResume() {
        super.onResume();
        runDailyResetIfNeeded();
    }



}