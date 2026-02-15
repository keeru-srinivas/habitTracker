import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';

import '../../domain/habit.dart';
import '../cubit/habit_cubit.dart';

class HabitCard extends StatelessWidget {
  final Habit habit;

  const HabitCard({super.key, required this.habit});

  @override
  Widget build(BuildContext context) {
    final totalEntries = habit.entries.length;
    final completedEntries =
        habit.entries.where((e) => e.completed).length;
    final progress = totalEntries == 0
        ? 0.0
        : completedEntries / totalEntries;
    return AnimatedContainer(
    duration: const Duration(milliseconds: 250),
      margin: const EdgeInsets.only(bottom: 16),
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: const Color(0xFF1A1D24),
        borderRadius: BorderRadius.circular(20),
        border: Border.all(
          color: Colors.white.withOpacity(0.05),
        ),
      ),
      child: Row(
        children: [
          Container(
            width: 14,
            height: 60,
            decoration: BoxDecoration(
              gradient: const LinearGradient(
                colors: [
                  Color(0xFF7C4DFF),
                  Color(0xFF00E5FF),
                ],
              ),
              borderRadius: BorderRadius.circular(6),
            ),
          ),
          const SizedBox(width: 16),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  habit.title,
                  style: const TextStyle(
                    fontSize: 18,
                    fontWeight: FontWeight.w600,
                  ),
                ),
                const SizedBox(height: 6),
                Text(
                  habit.frequency.toUpperCase(),
                  style: TextStyle(
                    fontSize: 12,
                    letterSpacing: 1.2,
                    color: Colors.white.withOpacity(0.5),
                  ),
                ),
              ],
            ),
          ),
      Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Stack(
            alignment: Alignment.center,
            children: [
              SizedBox(
                height: 42,
                width: 42,
                child: CircularProgressIndicator(
                  value: 0.6, // temporary fake progress
                  strokeWidth: 4,
                  backgroundColor: Colors.white.withOpacity(0.1),
                  valueColor: const AlwaysStoppedAnimation(
                    Color(0xFF7C4DFF),
                  ),
                ),
              ),
              const Icon(
                Icons.check,
                size: 18,
              ),
            ],
          ),
          const SizedBox(height: 6),
          GestureDetector(
            onTap: () {
              context.read<HabitCubit>().deleteHabit(habit.id);
            },
            child: const Icon(
              Icons.delete_outline,
              size: 18,
            ),
          ),
        ],
      ),

        ],
      ),
    );
  }
}
