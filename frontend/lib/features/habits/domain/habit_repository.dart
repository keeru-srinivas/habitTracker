
import 'habit.dart';
import 'habit_entry.dart';

abstract class HabitRepository {
  Future<List<Habit>> getHabits();

  Future<Habit> createHabit(Habit habit);

  Future<HabitEntry> addHabitEntry(String habitId, HabitEntry entry);

  Future<void> deleteHabit(String habitId);
}