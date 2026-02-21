import '../../../../core/errors/exceptions.dart';
import '../../../../core/errors/failure.dart';
import '../../../../core/network/api_client.dart';
import 'habit_dto.dart';
import 'habit_entry_dto.dart';
import '../domain/habit.dart';
import '../domain/habit_entry.dart';
import '../domain/habit_repository.dart';


class HabitRepositoryImpl implements HabitRepository {
  final ApiClient apiClient;

  HabitRepositoryImpl(this.apiClient);

  @override
  Future<List<Habit>> getHabits() async {
    try {
      const userId = "EtFsnFHpzxYVYuiiBzGEm354FIl2";

      final response = await apiClient.get('/users/$userId/habits');
      final List data = response.data as List;

      final now = DateTime.now();
      final yearStart = DateTime(now.year, 1, 1);
      final yearEnd = DateTime(now.year, 12, 31);

      final habits = <Habit>[];
      for (final json in data) {
        final habit = HabitDto.fromJson(json as Map<String, dynamic>).toDomain();
        final entries = await getHabitEntries(
          habit.id,
          startDate: yearStart,
          endDate: yearEnd,
        );
        habits.add(Habit(
          id: habit.id,
          userId: habit.userId,
          title: habit.title,
          frequency: habit.frequency,
          startDate: habit.startDate,
          isArchived: habit.isArchived,
          entries: entries,
        ));
      }
      return habits;
    } on ServerException catch (e) {
      throw ServerFailure(e.message);
    } on NetworkException catch (e) {
      throw NetworkFailure(e.message);
    } catch (e) {
      throw UnknownFailure(e.toString());
    }
  }

  @override
  Future<List<HabitEntry>> getHabitEntries(String habitId, {DateTime? startDate, DateTime? endDate}) async {
    try {
      var path = '/habits/$habitId/entries';
      final query = <String>[];
      if (startDate != null) query.add('startDate=${startDate.toIso8601String().split('T').first}');
      if (endDate != null) query.add('endDate=${endDate.toIso8601String().split('T').first}');
      if (query.isNotEmpty) path += '?${query.join('&')}';

      final response = await apiClient.get(path);
      final List data = response.data as List;
      return data
          .map((e) => HabitEntryDto.fromJson(e as Map<String, dynamic>).toDomain())
          .toList();
    } on ServerException catch (e) {
      throw ServerFailure(e.message);
    } on NetworkException catch (e) {
      throw NetworkFailure(e.message);
    } catch (e) {
      throw UnknownFailure(e.toString());
    }
  }

  @override
  Future<Habit> createHabit(Habit habit) async {
    try {
      // Backend HabitCreate expects only: title, frequency, startDate, userId
      final createPayload = {
        'title': habit.title,
        'frequency': habit.frequency,
        'startDate': habit.startDate.toIso8601String().split('T').first,
        'userId': habit.userId,
      };

      final response = await apiClient.post('/habits', data: createPayload);
      return HabitDto.fromJson(response.data as Map<String, dynamic>).toDomain();
    } on ServerException catch (e) {
      throw ServerFailure(e.message);
    } on NetworkException catch (e) {
      throw NetworkFailure(e.message);
    } catch (e) {
      throw UnknownFailure(e.toString());
    }
  }

  @override
  Future<HabitEntry> addHabitEntry(
      String habitId, HabitEntry entry) async {
    try {
      final payload = {
        'habitId': habitId,
        'date': entry.date,
        'completed': entry.completed,
      };
      final response = await apiClient.post(
        '/habit-entries',
        data: payload,
      );

      return HabitEntryDto.fromJson(response.data as Map<String, dynamic>).toDomain();
    } on ServerException catch (e) {
      throw ServerFailure(e.message);
    } on NetworkException catch (e) {
      throw NetworkFailure(e.message);
    } catch (e) {
      throw UnknownFailure(e.toString());
    }
  }

  @override
  Future<void> deleteHabit(String habitId) async {
    try {
      await apiClient.delete('/habits/$habitId');
    } on ServerException catch (e) {
      throw ServerFailure(e.message);
    } on NetworkException catch (e) {
      throw NetworkFailure(e.message);
    } catch (e) {
      throw UnknownFailure(e.toString());
    }
  }

  @override
  Future<void> toggleHabitCheck(
      String habitId,
      bool completed,
      ) async {
    try {
      final now = DateTime.now();
      final dateOnly = "${now.year}-${now.month.toString().padLeft(2, '0')}-${now.day.toString().padLeft(2, '0')}";
      await apiClient.post(
        '/habits/check',
        data: {
          "habitId": habitId,
          "date": dateOnly,
          "completed": completed,
        },
      );
    } on ServerException catch (e) {
      throw ServerFailure(e.message);
    } on NetworkException catch (e) {
      throw NetworkFailure(e.message);
    } catch (e) {
      throw UnknownFailure(e.toString());
    }
  }
}
