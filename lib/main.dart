import 'dart:convert';
import 'dart:typed_data';
import 'dart:ui';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:video_player/video_player.dart';
import 'package:webview_flutter/webview_flutter.dart';

void main() {
  runApp(MyApp());
}

class MyApp extends StatelessWidget {
  // This widget is the root of your application.
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'nes online',
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

  int webFinishedStates = 0;
  WebViewController webViewController;
  var windowWidth = MediaQueryData.fromWindow(window).size.width;
  var windowHeight = MediaQueryData.fromWindow(window).size.height;

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
                onPressed: start,
                child: Text(
                  "getJson",
                  style: TextStyle(color: Colors.white),
                )),
            Container(
              width: windowWidth,
              height: windowWidth,
              child: Offstage(
                //坑： webview loaddata加载过程中，黑屏，所以加载后再显示
                offstage: webFinishedStates == 0,
                child: WebView(
                  onWebViewCreated: (controller) {
                    webViewController = controller;
                    loadHtml();
                  },
                  javascriptMode: JavascriptMode.unrestricted,
                  onPageFinished: (String url) {
                    webFinishedStates = 1;
                    setState(() {});
                  },
                  javascriptChannels: <JavascriptChannel>[
                    _toasterJavascriptChannel(context),
                  ].toSet(),
                ),
              ),
            )
          ],
        ),
      ), // This trailing comma makes auto-formatting nicer for build methods.
    );
  }

  Future<String> getFileData(String path) async {
    return await rootBundle.loadString(path);
  }

  void loadHtml() async {
    print("loadHtml");
    String data = await getFileData("assets/nes-embed.html");
    await webViewController.loadData(data);
    print("loadHtml finished");
  }

  JavascriptChannel _toasterJavascriptChannel(BuildContext context) {
    return JavascriptChannel(
        name: 'print',
        onMessageReceived: (JavascriptMessage message) {
          print("通信=${message.message}");
        });
  }

  void start() {}
  void test(){
    VideoPlayerController controller;
  }
}
