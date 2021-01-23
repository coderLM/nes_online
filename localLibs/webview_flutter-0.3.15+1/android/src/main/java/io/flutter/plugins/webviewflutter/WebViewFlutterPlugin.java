// Copyright 2018 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package io.flutter.plugins.webviewflutter;

import androidx.annotation.NonNull;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.PluginRegistry.Registrar;

/** WebViewFlutterPlugin */
public class WebViewFlutterPlugin  implements FlutterPlugin {

  private FlutterCookieManager flutterCookieManager;
  /** Plugin registration. */
  public static void registerWith(Registrar registrar) {
    registrar
            .platformViewRegistry()
            .registerViewFactory(
                    "plugins.flutter.io/webview",
                    new WebViewFactory(registrar.messenger(), registrar.view()));
    new FlutterCookieManager(registrar.messenger());
  }

  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding binding) {
    BinaryMessenger messenger = binding.getBinaryMessenger();
    binding
            .getPlatformViewRegistry()
            .registerViewFactory(
                    "plugins.flutter.io/webview", new WebViewFactory(messenger, /*containerView=*/ null));
    flutterCookieManager = new FlutterCookieManager(messenger);
  }

  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
    if (flutterCookieManager == null) {
      return;
    }

    flutterCookieManager.dispose();
    flutterCookieManager = null;
  }
}
