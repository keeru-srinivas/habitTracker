import '../domain/habit_entry.dart';

class HabitEntryDto{
  final String id;
  final String habitId;
  final String date;
  final bool completed;
  final String? completedAt;

  HabitEntryDto({
    required this.id,
    required this.habitId,
    required this.date,
    required this.completed,
    this.completedAt,
});

  factory HabitEntryDto.fromJson(Map<String,dynamic>json){
    return HabitEntryDto(
      id: json['id'] as String,
      habitId:json['habitId'] as String,
      date: json['date'] as String,
      completed: json['completed'] as bool,
      completedAt: json['completedAt'] as String?
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'habitId': habitId,
      'date': date,
      'completed': completed,
      'completedAt': completedAt,
    };
  }

  HabitEntry toDomain(){
    return HabitEntry(
      id: id,
      habitId: habitId,
      date: date,
      completed: completed,
      completedAt: completedAt != null ? DateTime.parse(completedAt!): null,
    );
  }
  factory HabitEntryDto.fromDomain(HabitEntry entry) {
    return HabitEntryDto(
      id: entry.id,
      habitId: entry.habitId,
      date: entry.date, // already String in your domain
      completed: entry.completed,
      completedAt:
      entry.completedAt?.toIso8601String(),
    );
  }





}