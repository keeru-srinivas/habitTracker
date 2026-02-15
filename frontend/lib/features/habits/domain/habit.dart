
import '../data/habit_entry_dto.dart';
import 'habit_entry.dart';

class Habit {
  final String id;
  final String userId;
  final String title;
  final String frequency;
  final DateTime startDate;
  final bool isArchived;
  final List<HabitEntry> entries;



  Habit({
    required this.id,
    required this.userId,
    required this.title,
    required this.frequency,
    required this.startDate,
    required this.isArchived,
    required this.entries,
  });
}