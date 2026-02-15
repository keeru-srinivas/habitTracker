import 'dart:io';

import 'package:flutter/foundation.dart';

class Env{
  static String get baseUrl{
    if (kIsWeb){
      return 'http://localhost:8000/api';
    }
    if(Platform.isAndroid){
      return 'http://10.0.2.2:8000/api';
    }
    if (Platform.isIOS) {
      return 'http://127.0.0.1:8000/api';
    }
    return 'http://localhost:8000/api';
  }
}