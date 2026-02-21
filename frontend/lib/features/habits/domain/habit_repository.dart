import 'habit.dart';
import 'habit_entry.dart';

abstract class HabitRepository {
  Future<List<Habit>> getHabits();

  Future<Habit> createHabit(Habit habit);

  Future<HabitEntry> addHabitEntry(String habitId, HabitEntry entry);

  Future<List<HabitEntry>> getHabitEntries(String habitId, {DateTime? startDate, DateTime? endDate});

  Future<void> deleteHabit(String habitId);

  Future<void> toggleHabitCheck(String habitId, bool completed);
}