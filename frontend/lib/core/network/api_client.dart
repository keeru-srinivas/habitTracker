import 'package:dio/dio.dart';

import '../config/env.dart';
import 'package:dio/dio.dart';

import '../errors/exceptions.dart';

class ApiClient {
  late final Dio _dio;

  ApiClient() {
    _dio = Dio(
      BaseOptions(
        baseUrl: Env.baseUrl,
        headers: {'Content-Type': 'application/json'},
      ),
    );
  }

  Future<Response> get(String path) async {
    try {
      return await _dio.get(path);
    } on DioException catch (e) {
      throw _handleDioError(e);
    } catch (e) {
      throw UnknownException(e.toString());
    }
  }

  Future<Response> post(String path, {dynamic data}) async {
    try {
      return await _dio.post(path, data: data);
    } on DioException catch (e) {
      throw _handleDioError(e);
    } catch (e) {
      throw UnknownException(e.toString());
    }
  }

  Future<Response> put(String path, {dynamic data}) async {
    try {
      return await _dio.put(path, data: data);
    } on DioException catch (e) {
      throw _handleDioError(e);
    } catch (e) {
      throw UnknownException(e.toString());
    }
  }

  Future<Response> delete(String path, {dynamic data}) async {
    try {
      return await _dio.delete(path, data: data);
    } on DioException catch (e) {
      throw _handleDioError(e);
    } catch (e) {
      throw UnknownException(e.toString());
    }
  }

  Exception _handleDioError(DioException e) {
    if (e.type == DioExceptionType.connectionTimeout ||
        e.type == DioExceptionType.receiveTimeout ||
        e.type == DioExceptionType.sendTimeout) {
      return NetworkException("Connection timeout");
    }

    if (e.type == DioExceptionType.connectionError) {
      return NetworkException("No internet connection");
    }

    if (e.response != null) {
      final data = e.response?.data;
      if (data is Map<String, dynamic>) {
        // FastAPI uses "detail" for HTTPException and validation errors
        final detail = data['detail'];
        if (detail is String) {
          return ServerException(detail);
        }
        if (detail is List && detail.isNotEmpty) {
          final first = detail.first;
          final msg = first is Map ? (first['msg'] ?? first['message']) : detail.toString();
          return ServerException(msg?.toString() ?? "Validation error");
        }
        return ServerException(data['message'] as String? ?? "Server error");
      }
      return ServerException("Server error");
    }
    return UnknownException("Unexpected error occurred");
  }
}
