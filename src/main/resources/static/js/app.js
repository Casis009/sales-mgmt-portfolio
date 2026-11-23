// 画面共通の動的処理（画面一覧 5.2）。サーバー側のチェック・二重送信防止を省略する理由にはしない。
(function () {
  'use strict';

  // 確認ダイアログ（No.9）：data-confirm を付けたボタン・フォームは、送信前に確認する
  document.addEventListener('submit', function (event) {
    var form = event.target;
    var submitter = event.submitter;
    var message = (submitter && submitter.dataset.confirm) || form.dataset.confirm;
    if (message && !window.confirm(message)) {
      event.preventDefault();
      return;
    }
    // 二重送信防止（No.5）：data-once を付けたフォームは、送信後にボタンを非活性にする
    if (form.hasAttribute('data-once')) {
      // 押したボタンの name/value を送信内容に残すため、非活性化は送信処理の後に行う
      window.setTimeout(function () {
        form.querySelectorAll('button[type=submit], input[type=submit]').forEach(function (b) {
          b.disabled = true;
        });
      }, 0);
    }
  });
})();
