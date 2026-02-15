import 'package:flutter/material.dart';

class AppTheme {
  static ThemeData darkTheme = ThemeData(
    brightness: Brightness.dark,
    scaffoldBackgroundColor: const Color(0xFF0F1117),
    primaryColor: const Color(0xFF7C5CFF),
    colorScheme: const ColorScheme.dark(
      primary: Color(0xFF7C5CFF),
      secondary: Color(0xFF9F7AEA),
      surface: Color(0xFF1A1D25),
    ),
    cardTheme: CardThemeData(
      color: const Color(0xFF1A1D25),
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(16),
      ),
      elevation: 6,
    ),
    floatingActionButtonTheme: const FloatingActionButtonThemeData(
      backgroundColor: Color(0xFF7C5CFF),
    ),
    textTheme: const TextTheme(
      headlineMedium: TextStyle(
        fontSize: 26,
        fontWeight: FontWeight.bold,
      ),
      bodyLarge: TextStyle(
        fontSize: 16,
        color: Colors.white70,
      ),
    ),
  );
}
