import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import '../widgets/habit_card.dart';
import '../../domain/habit.dart';
import '../../data/habit_repository_impl.dart';
import '../../../../core/network/api_client.dart';
import '../cubit/habit_cubit.dart';
import '../cubit/habit_state.dart';

class HabitPage extends StatelessWidget {
  const HabitPage({super.key});

  @override
  Widget build(BuildContext context) {
    return BlocProvider(
      create: (_) =>
      HabitCubit(HabitRepositoryImpl(ApiClient()))..fetchHabits(),
      child: const HabitView(),
    );
  }
}

class HabitView extends StatelessWidget {
  const HabitView({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        elevation: 0,
        backgroundColor: Colors.transparent,
        centerTitle: true,
        title: const Text(
          "My Habits",
          style: TextStyle(
            fontWeight: FontWeight.bold,
            fontSize: 22,
          ),
        ),
      ),

      // ================= BODY =================

      body: BlocBuilder<HabitCubit, HabitState>(
        builder: (context, state) {

          if (state is HabitLoading) {
            return const Center(child: CircularProgressIndicator());
          }

          if (state is HabitError) {
            return Center(child: Text(state.message));
          }

          if (state is HabitLoaded) {
            final habits = state.habits;

            return SafeArea(
              child: Padding(
                padding: const EdgeInsets.symmetric(horizontal: 24),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [

                    const SizedBox(height: 24),

                    // Dashboard Header
                    const Text(
                      "Dashboard",
                      style: TextStyle(
                        fontSize: 28,
                        fontWeight: FontWeight.bold,
                      ),
                    ),

                    const SizedBox(height: 8),

                    Text(
                      "Track your daily momentum",
                      style: TextStyle(
                        color: Colors.white.withOpacity(0.6),
                      ),
                    ),

                    const SizedBox(height: 28),

                    // Stats Row
                    Row(
                      children: [
                        Expanded(
                          child: _StatCard(
                            title: "Total",
                            value: habits.length.toString(),
                          ),
                        ),
                        const SizedBox(width: 12),
                        Expanded(
                          child: _StatCard(
                            title: "Active",
                            value: habits
                                .where((h) => !h.isArchived)
                                .length
                                .toString(),
                          ),
                        ),
                        const SizedBox(width: 12),
                        const Expanded(
                          child: _StatCard(
                            title: "Streak",
                            value: "12",
                          ),
                        ),
                      ],
                    ),

                    const SizedBox(height: 32),

                    const Text(
                      "Your Habits",
                      style: TextStyle(
                        fontSize: 18,
                        fontWeight: FontWeight.w600,
                      ),
                    ),

                    const SizedBox(height: 16),

                    // Habit List
                    Expanded(
                      child: habits.isEmpty
                          ? const Center(
                        child: Text("No habits yet"),
                      )
                          : ListView.builder(
                        itemCount: habits.length,
                        itemBuilder: (context, index) {
                          return HabitCard(habit: habits[index], colorIndex: index);
                        },
                      ),
                    ),
                  ],
                ),
              ),
            );
          }

          return const SizedBox();
        },
      ),

      // ================= FAB =================

      floatingActionButton: Container(
        decoration: BoxDecoration(
          borderRadius: BorderRadius.circular(30),
          gradient: const LinearGradient(
            colors: [
              Color(0xFF7C4DFF),
              Color(0xFF00E5FF),
            ],
          ),
        ),
        child: FloatingActionButton(
          backgroundColor: Colors.transparent,
          elevation: 0,
          onPressed: () {
            _showCreateDialog(context);
          },
          child: const Icon(Icons.add),
        ),
      ),
    );
  }

  // ================= CREATE HABIT DIALOG =================

  void _showCreateDialog(BuildContext context) {
    final controller = TextEditingController();

    showDialog(
      context: context,
      builder: (_) => AlertDialog(
        backgroundColor: const Color(0xFF1A1D25),
        title: const Text("Create Habit"),
        content: TextField(
          controller: controller,
          decoration: const InputDecoration(
            hintText: "Enter habit title",
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text("Cancel"),
          ),
          ElevatedButton(
            onPressed: () {
              final habit = Habit(
                id: '',
                userId: "EtFsnFHpzxYVYuiiBzGEm354FIl2", // Your test user
                title: controller.text,
                frequency: 'daily',
                startDate: DateTime.now(),
                isArchived: false,
                entries: [],
              );

              context.read<HabitCubit>().createHabit(habit);
              Navigator.pop(context);
            },
            child: const Text("Create"),
          ),
        ],
      ),
    );
  }
}

// ================= STAT CARD =================

class _StatCard extends StatefulWidget {
  final String title;
  final String value;

  const _StatCard({
    required this.title,
    required this.value,
  });

  @override
  State<_StatCard> createState() => _StatCardState();
}

class _StatCardState extends State<_StatCard>
    with SingleTickerProviderStateMixin {

  late AnimationController _controller;
  late Animation<double> _animation;

  @override
  void initState() {
    super.initState();

    final targetValue = double.tryParse(widget.value) ?? 0;

    _controller = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 800),
    );

    _animation = Tween<double>(
      begin: 0,
      end: targetValue,
    ).animate(
      CurvedAnimation(
        parent: _controller,
        curve: Curves.easeOut,
      ),
    );

    _controller.forward();
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: const Color(0xFF1A1D25),
        borderRadius: BorderRadius.circular(18),
        border: Border.all(
          color: Colors.white.withOpacity(0.05),
        ),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          AnimatedBuilder(
            animation: _animation,
            builder: (context, child) {
              return Text(
                _animation.value.toInt().toString(),
                style: const TextStyle(
                  fontSize: 24,
                  fontWeight: FontWeight.bold,
                ),
              );
            },
          ),
          const SizedBox(height: 6),
          Text(
            widget.title,
            style: TextStyle(
              color: Colors.white.withOpacity(0.6),
              fontSize: 12,
              letterSpacing: 1.2,
            ),
          ),
        ],
      ),
    );
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }
}
