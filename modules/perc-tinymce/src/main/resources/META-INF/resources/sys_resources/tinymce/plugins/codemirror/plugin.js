/**
 * plugin.js
 *
 * Copyright 2013 Web Power, www.webpower.nl
 * @author Arjan Haverkamp
 * Updated 2026 Percussion Software, Inc. for TinyMCE 6+ compatibility.
 */

/* jshint unused:false */
/* global tinymce:true */

tinymce.PluginManager.add("codemirror", function (editor, url) {
  // Register the custom 'codemirror' option so TinyMCE 6+ validates it correctly.
  editor.options.register("codemirror", {
    processor: "object",
    default: {
      indentOnInit: true,
      fullscreen: false,
      saveCursorPosition: true,
      config: {
        lineNumbers: true,
        autofocus: true,
        screenReaderLabel: "HTML Source Code Editor",
      },
    },
  });

  function showSourceEditor() {
    editor.focus();
    editor.selection.collapse(true);

    //Disable Parent Dialog Buttons
    toggleParentDialogButton(true);

    // Insert caret marker
    if (editor.options.get("codemirror").saveCursorPosition) {
      editor.selection.setContent(
        '<span style="display: none;" class="CmCaReT">&#x0;</span>',
      );
    }

    var codemirrorWidth = window.innerWidth;
    if (editor.options.get("codemirror").width) {
      codemirrorWidth = editor.options.get("codemirror").width;
    }

    var codemirrorHeight = window.innerHeight;
    if (editor.options.get("codemirror").height) {
      codemirrorHeight = editor.options.get("codemirror").height;
    }

    var buttonsConfig = [
      {
        type: "custom",
        text: "Ok",
        name: "codemirrorOk",
        primary: true,
      },
      {
        type: "cancel",
        text: "Cancel",
        name: "codemirrorCancel",
      },
    ];

    var config = {
      title: "HTML source code",
      url: url + "/source.html",
      width: codemirrorWidth,
      height: codemirrorHeight,
      resizable: true,
      maximizable: true,
      fullScreen: editor.options.get("codemirror").fullscreen,
      saveCursorPosition: false,
      inline: "bottom",
      buttons: buttonsConfig,
      onClose: closeDialog,
      onAction: function (dialogApi, actionData) {
        if (actionData.name === "codemirrorOk") {
          var doc = document.querySelectorAll(
            ".tox-dialog__body-iframe iframe",
          )[0];
          doc.contentWindow.submit();
          editor.undoManager.add();
          win.close();
        } else {
          win.close();
        }
      },
    };

    var win = editor.windowManager.openUrl(config);

    if (editor.options.get("codemirror").fullscreen) {
      win.fullscreen(true);
    }
  }

  //Enable Parent Dialog Buttons
  function closeDialog() {
    toggleParentDialogButton(false);
  }

  //Enable/Disable Parent Dialog buttons based on passed in flag
  function toggleParentDialogButton(disable) {
    var saveBtn = window.top.$("#perc-content-edit-save-button")[0];
    if (saveBtn) {
      saveBtn.disabled = disable;
      if (disable) {
        saveBtn.classList.add("disabled");
      } else {
        saveBtn.classList.remove("disabled");
      }
    }
    var cancelBtn = window.top.$("#perc-content-edit-cancel-button")[0];
    if (cancelBtn) {
      cancelBtn.disabled = disable;
      if (disable) {
        cancelBtn.classList.add("disabled");
      } else {
        cancelBtn.classList.remove("disabled");
      }
    }
  }

  editor.ui.registry.addButton("code", {
    icon: "sourcecode",
    title: "Source code",
    tooltip: "Source code",
    onAction: showSourceEditor,
  });

  editor.ui.registry.addMenuItem("code", {
    icon: "sourcecode",
    text: "Source code",
    onAction: showSourceEditor,
    context: "tools",
  });
});
