
import '../domain/habit.dart';
import 'habit_entry_dto.dart';


class HabitDto{
  final String id;
  final String userId;
  final String title;
  final String frequency;
  final String startDate;
  final bool isArchived;
  final List<HabitEntryDto> entries;

  HabitDto({
    required this.id,
    required this.userId,
    required this.title,
    required this.frequency,
    required this.startDate,
    required this.isArchived,
    required this.entries,

});

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'userId': userId,
      'title': title,
      'frequency': frequency,
      'startDate': startDate,
      'isArchived': isArchived,
      'entries': entries.map((e) => e.toJson()).toList(),
    };
  }

  factory HabitDto.fromJson(Map<String, dynamic> json) {
    return HabitDto(
      id: json['id'] as String,
      userId: json['userId'] as String,
      title: json['title'] as String,
      frequency: json['frequency'] as String,
      startDate: json['startDate'] as String,
      isArchived: json['isArchived'] as bool,
      entries: json['entries'] != null
          ? (json['entries'] as List<dynamic>)
                .map((e) => HabitEntryDto.fromJson(e as Map<String, dynamic>))
                .toList()
          : [],
    );
  }

  Habit toDomain(){
    return Habit(
      id: id,
      userId: userId,
      title: title,
      frequency: frequency,
      startDate: DateTime.parse(startDate),
      isArchived: isArchived,
      entries: entries.map((e)=> e.toDomain()).toList(),
    );
  }
  factory HabitDto.fromDomain(Habit habit) {
    return HabitDto(
      id: habit.id,
      userId: habit.userId,
      title: habit.title,
      frequency: habit.frequency,
      startDate: habit.startDate.toIso8601String(),
      isArchived: habit.isArchived,
      entries: habit.entries
          .map((e) => HabitEntryDto.fromDomain(e))
          .toList(),
    );
  }



}