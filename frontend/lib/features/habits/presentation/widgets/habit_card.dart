import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';

import '../../domain/habit.dart';
import '../cubit/habit_cubit.dart';

/// Colors used for heatmap "completed" cells (one per habit for variety)
const _heatmapColors = [
  Color(0xFF64B5F6), // light blue
  Color(0xFFFFB74D), // amber
  Color(0xFFF48FB1), // pink
  Color(0xFF81C784), // light green
  Color(0xFFB39DDB), // light purple
  Color(0xFF90A4AE), // blue grey
];

class HabitCard extends StatelessWidget {
  final Habit habit;
  final int colorIndex;

  const HabitCard({super.key, required this.habit, this.colorIndex = 0});

  @override
  Widget build(BuildContext context) {
    final completedDates = habit.entries
        .where((e) => e.completed)
        .map((e) => e.date)
        .toSet();
    final year = DateTime.now().year;
    final startOfYear = DateTime(year, 1, 1);
    final endOfYear = DateTime(year, 12, 31);
    final habitStart = habit.startDate.isBefore(startOfYear) ? startOfYear : habit.startDate;
    final totalDays = endOfYear.difference(habitStart).inDays + 1;
    final completedInYear = completedDates.where((d) {
      if (d.length >= 10) {
        final y = int.tryParse(d.substring(0, 4));
        return y == year && d.compareTo(habitStart.toIso8601String().split('T').first) >= 0
            && d.compareTo(endOfYear.toIso8601String().split('T').first) <= 0;
      }
      return false;
    }).length;
    final totalDaysClamped = totalDays <= 0 ? 1 : totalDays;
    final progressFraction = (completedInYear / totalDaysClamped).clamp(0.0, 1.0);
    final progressPercent = (progressFraction * 100).round();
    final today = DateTime.now().toIso8601String().split('T').first;
    final hasCompletedToday = completedDates.any((d) => d == today);

    final heatmapColor = _heatmapColors[colorIndex % _heatmapColors.length];

    return AnimatedContainer(
      duration: const Duration(milliseconds: 250),
      margin: const EdgeInsets.only(bottom: 16),
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: const Color(0xFF1A1D24),
        borderRadius: BorderRadius.circular(20),
        border: Border.all(color: Colors.white.withOpacity(0.05)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Container(
                width: 14,
                height: 44,
                decoration: BoxDecoration(
                  gradient: const LinearGradient(
                    colors: [Color(0xFF7C4DFF), Color(0xFF00E5FF)],
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
                    const SizedBox(height: 4),
                    Text(
                      _goalLabel(habit),
                      style: TextStyle(
                        fontSize: 12,
                        letterSpacing: 0.5,
                        color: Colors.white.withOpacity(0.5),
                      ),
                    ),
                  ],
                ),
              ),
              Text(
                '$progressPercent%',
                style: TextStyle(
                  fontSize: 16,
                  fontWeight: FontWeight.w600,
                  color: Colors.white.withOpacity(0.9),
                ),
              ),
              const SizedBox(width: 12),
              Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  GestureDetector(
                    onTap: () => context.read<HabitCubit>().toggleHabit(habit),
                    child: Stack(
                      alignment: Alignment.center,
                      children: [
                        SizedBox(
                          height: 42,
                          width: 42,
                          child: CircularProgressIndicator(
                            value: progressFraction,
                            strokeWidth: 4,
                            backgroundColor: Colors.white.withOpacity(0.1),
                            valueColor: const AlwaysStoppedAnimation(
                              Color(0xFF7C4DFF),
                            ),
                          ),
                        ),
                        Icon(
                          hasCompletedToday
                              ? Icons.check_circle
                              : Icons.radio_button_unchecked,
                          size: 18,
                          color: hasCompletedToday
                              ? Colors.greenAccent
                              : Colors.white,
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(height: 6),
                  GestureDetector(
                    onTap: () => context.read<HabitCubit>().deleteHabit(habit.id),
                    child: Icon(
                      Icons.delete_outline,
                      size: 18,
                      color: Colors.white.withOpacity(0.5),
                    ),
                  ),
                ],
              ),
            ],
          ),
          const SizedBox(height: 16),
          _HeatmapGrid(
            year: year,
            completedDates: completedDates,
            cellColor: heatmapColor,
            habitStart: habitStart,
          ),
        ],
      ),
    );
  }

  String _goalLabel(Habit h) {
    final f = h.frequency.toLowerCase();
    if (f == 'daily') return 'Every day';
    if (f == 'weekly') return 'Per week';
    return h.frequency;
  }
}

class _HeatmapGrid extends StatelessWidget {
  final int year;
  final Set<String> completedDates;
  final Color cellColor;
  final DateTime habitStart;

  const _HeatmapGrid({
    required this.year,
    required this.completedDates,
    required this.cellColor,
    required this.habitStart,
  });

  @override
  Widget build(BuildContext context) {
    const rows = 7;
    const cols = 53;
    const cellSize = 10.0;
    const spacing = 2.0;
    final start = DateTime(year, 1, 1);
    final end = DateTime(year, 12, 31);
    final totalDays = end.difference(start).inDays + 1;

    return SingleChildScrollView(
      scrollDirection: Axis.horizontal,
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: List.generate(cols, (col) {
          return Column(
            mainAxisSize: MainAxisSize.min,
            children: List.generate(rows, (row) {
              final dayOffset = col * 7 + row;
              if (dayOffset >= totalDays) {
                return Padding(
                  padding: const EdgeInsets.all(spacing / 2),
                  child: SizedBox(width: cellSize, height: cellSize),
                );
              }
              final date = start.add(Duration(days: dayOffset));
              final dateStr = '${date.year}-${date.month.toString().padLeft(2, '0')}-${date.day.toString().padLeft(2, '0')}';
              final isCompleted = completedDates.contains(dateStr);
              final isBeforeStart = date.isBefore(habitStart);

              return Padding(
                padding: const EdgeInsets.all(spacing / 2),
                child: Container(
                  width: cellSize,
                  height: cellSize,
                  decoration: BoxDecoration(
                    color: isBeforeStart
                        ? Colors.white.withOpacity(0.03)
                        : (isCompleted ? cellColor : Colors.white.withOpacity(0.08)),
                    borderRadius: BorderRadius.circular(2),
                  ),
                ),
              );
            }),
          );
        }),
      ),
    );
  }
}
