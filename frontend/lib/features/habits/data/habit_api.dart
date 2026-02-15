import 'dart:convert';
import 'package:flutter/cupertino.dart';

import '../../../core/network/api_client.dart';
import 'habit_dto.dart';

class HabitApi{
  final ApiClient apiClient;
  HabitApi(this.apiClient);
  Future<List<HabitDto>>fetchHabits() async{
    final response = await apiClient.get('/habits');
    final data = response.data as List<dynamic>;
    return data
        .map((json) =>
    HabitDto.fromJson(json as Map<String, dynamic>))
        .toList();
  }

  Future<void> createHabit({
    required String title,
    required String frequency,
    required DateTime startDate,
}) async{
    await apiClient.post(
      '/habits',
      data: {
        'title': title,
        'frequency': frequency,
        'startDate': startDate.toIso8601String(),
      },
    );
  }
}