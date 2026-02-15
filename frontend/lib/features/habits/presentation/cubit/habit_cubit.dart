import 'package:flutter_bloc/flutter_bloc.dart';
import '../../domain/habit.dart';
import '../../domain/habit_repository.dart';
import 'habit_state.dart';
import '../../../../core/errors/failure.dart';

class HabitCubit extends Cubit<HabitState> {
  final HabitRepository repository;

  HabitCubit(this.repository) : super(HabitInitial());

  Future<void> fetchHabits() async {
    emit(HabitLoading());

    try {
      final habits = await repository.getHabits();
      emit(HabitLoaded(habits));
    } catch (e) {
      if (e is Failure) {
        emit(HabitError(e.message));
      } else {
        emit(HabitError("Unexpected error occurred"));
      }
    }
  }

  Future<void> createHabit(Habit habit) async {
    emit(HabitLoading());

    try {
      await repository.createHabit(habit);
      await fetchHabits(); // refresh list
    } catch (e) {
      if (e is Failure) {
        emit(HabitError(e.message));
      } else {
        emit(HabitError("Unexpected error occurred"));
      }
    }
  }

  Future<void> deleteHabit(String habitId) async {
    emit(HabitLoading());

    try {
      await repository.deleteHabit(habitId);
      await fetchHabits();
    } catch (e) {
      if (e is Failure) {
        emit(HabitError(e.message));
      } else {
        emit(HabitError("Unexpected error occurred"));
      }
    }
  }
}
