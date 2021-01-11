import 'dart:convert';
import 'dart:io';
import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:video_player/video_player.dart';

void main() {
  runApp(MyApp());
}

class MyApp extends StatelessWidget {
  // This widget is the root of your application.
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Flutter Demo',
      theme: ThemeData(
        primarySwatch: Colors.blue,
        visualDensity: VisualDensity.adaptivePlatformDensity,
      ),
      home: MyHomePage(title: 'Flutter Demo Home Page'),
    );
  }
}

class MyHomePage extends StatefulWidget {
  MyHomePage({Key key, this.title}) : super(key: key);
  final String title;

  @override
  _MyHomePageState createState() => _MyHomePageState();
}

class _MyHomePageState extends State<MyHomePage> {
  @override
  void initState() {
    super.initState();
  }

  Uint8List list8;

  void readJson() async {
    String str = await rootBundle.loadString("assets/data/nes_frame.json");
    Map<String,dynamic> data = jsonDecode(str);
    List<dynamic> list64 = data.values.toList();

    Uint32List list32 = Uint32List.fromList(list64.cast<int>());

    list8 = list32.buffer.asUint8List();
    print('readJosn len:::'+list64.length.toString());
    print('readJosn len:::'+list8.length.toString());
    print('list32[0]:::'+list32[0].toString());
    print('list8[0]:::'+list8[0].toString());
    print('list8[1]:::'+list8[1].toString());
    print('list8[2]:::'+list8[2].toString());
    print('list8[3]:::'+list8[3].toString());
    print('list8[4]:::'+list8[4].toString());
    print('list8[5]:::'+list8[5].toString());
    print('list8[6]:::'+list8[6].toString());
    print('list8[7]:::'+list8[7].toString());
    setState(() {});
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(widget.title),
      ),
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: <Widget>[
            FlatButton(
                color: Colors.black,
                onPressed: readJson,
                child: Text(
                  "getJson",
                  style: TextStyle(color: Colors.white),
                )),
            list8 == null
                ? Container()
                : Image.memory(
                    list8,
                    // width: 256,
                    // height: 240,
                  ),
          ],
        ),
      ), // This trailing comma makes auto-formatting nicer for build methods.
    );
  }
}
