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

      final response =
      await apiClient.get('/users/$userId/habits');


      final List data = response.data;

      return data
          .map((json) => HabitDto.fromJson(json).toDomain())
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
      final dto = HabitDto.fromDomain(habit);

      final response =
      await apiClient.post('/habits', data: dto.toJson());

      return HabitDto.fromJson(response.data).toDomain();
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
      final dto = HabitEntryDto.fromDomain(entry);

      final response = await apiClient.post(
        '/habits/$habitId/entries',
        data: dto.toJson(),
      );

      return HabitEntryDto.fromJson(response.data).toDomain();
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
}
