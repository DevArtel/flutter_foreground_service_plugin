import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter_foreground_plugin/flutter_foreground_plugin.dart';

void main() => runApp(MyApp());

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  @override
  void initState() {
    startForegroundService();
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Foreground Service Example'),
        ),
        body: Center(
          child: Column(
            children: <Widget>[
              ElevatedButton(
                child: Text("START"),
                onPressed: () {
                  startForegroundService();
                },
              ),
              ElevatedButton(
                child: Text("STOP"),
                onPressed: () async {
                  await FlutterForegroundPlugin.stopForegroundService();
                },
              ),
              ElevatedButton(
                child: Text("Force Crash"),
                onPressed: () {
                  exit(1);
                },
              )
            ],
          ),
        ),
      ),
    );
  }
}

void startForegroundService() async {
  await FlutterForegroundPlugin.setServiceMethodInterval(seconds: 5);
  await FlutterForegroundPlugin.setServiceMethod(globalForegroundService);
  await FlutterForegroundPlugin.startForegroundService(
    onStarted: () {
      print("Foreground on Started");
    },
    onStopped: () {
      print("Foreground on Stopped");
    },
    title: "Flutter Foreground Service",
    // content: "This is Content",
    iconName: "ic_stat_hot_tub",
    channelName: "This is channel name",
  );
}

void globalForegroundService() {
  debugPrint("current datetime is ${DateTime.now()}");
}
