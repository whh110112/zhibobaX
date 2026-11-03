package com.whh11.zb8lite.util

/**
 * 直播吧 WebView 页面净化与主题适配脚本。
 * 1. 彻底拦截并隐藏顶部/底部"打开直播吧APP"横幅、下载引导、浮动推广层；
 * 2. 根除内联与动态插入的下载弹窗；
 * 3. 完美适配白天与黑夜模式：注入高对比度文本配色，彻底消除黑色底黑文字无法看清的缺陷。
 */
object Injector {

    private fun getCss(isDark: Boolean): String {
        val themeCss = if (isDark) {
            """
            html, body, .wrapper, .main, .content, .head, .vct-comment, .relation-news,
            .comment-box, .comment-list, .comment-title, .detail-container, .lists, .hot-lists {
                background-color: #0B0E14 !important;
                color: #EDF1F7 !important;
            }
            p, span, div, h1, h2, h3, a, li, ._content, .p1, .p2, .child-content,
            .title, .createtime, .from-name, .text-title, ._title, .comment-content, .info {
                color: #EDF1F7 !important;
            }
            .user-name, .user-name-zone, .reply {
                color: #2E9FFF !important;
            }
            .children-zone, .child-item {
                background-color: #141922 !important;
                color: #EDF1F7 !important;
            }
            .time, .createtime, ._bottom, ._val, .text-pinglun-count, .text-time, .dot {
                color: #8A94A6 !important;
            }
            """
        } else {
            """
            html, body, .wrapper, .main, .content, .head, .vct-comment, .relation-news,
            .comment-box, .comment-list, .comment-title, .detail-container, .lists, .hot-lists {
                background-color: #FFFFFF !important;
                color: #1E293B !important;
            }
            p, span, div, h1, h2, h3, a, li, ._content, .p1, .p2, .child-content,
            .title, .createtime, .from-name, .text-title, ._title, .comment-content, .info {
                color: #1E293B !important;
            }
            .user-name, .user-name-zone, .reply {
                color: #0084FF !important;
            }
            .children-zone, .child-item {
                background-color: #F8FAFC !important;
                color: #1E293B !important;
            }
            .time, .createtime, ._bottom, ._val, .text-pinglun-count, .text-time, .dot {
                color: #64748B !important;
            }
            """
        }

        return """
        .top-header, .foot-area, .tip-fixed, .tip, .alert-box, .bind-box,
        .to-app, .btn-open, [class*="top-header"], [class*="foot-area"],
        [class*="download-app"], [class*="downloadApp"], [class*="open-app"], [class*="openApp"],
        [class*="app-guide"], [class*="appGuide"], [class*="client-guide"],
        .download-fixed, .open-fixed, [class*="footer-tip"],
        .adv, .m_adv, .adv_tip, .advert_android, .advertframe,
        .ad_left, .ad_right, .ad_left_close, .ad_right_close,
        .qrcode-banner, .bd_adv, [id*="m_a_d_v"], [id*="addDesktop"],
        header.header, [class*="app-launch-banner"] {
            display: none !important;
            height: 0 !important;
            min-height: 0 !important;
            visibility: hidden !important;
            opacity: 0 !important;
            pointer-events: none !important;
        }
        html, body {
            padding-top: 0 !important;
            margin-top: 0 !important;
            overscroll-behavior-y: contain;
            -webkit-tap-highlight-color: transparent;
        }
        $themeCss
        """
    }

    private const val SCRIPT = """
(function(){
  window.openApp = function(){ return false; };
  window.launchApp = function(){ return false; };
  window.downloadApp = function(){ return false; };
  if (window.global) {
    window.global.openApp = function(){ return false; };
    window.global.delayCreateElem = function(){};
  }

  var CSS = __CSS__;
  var BAD = ['下载APP','下载App','下载app','打开APP','打开App','立即下载','打开客户端',
             '前往APP','用APP打开','去APP','下载客户端','安装APP','安装App','打开应用',
             '打开直播吧APP','体育资讯，快人一步','快人一步'];

  function addStyle(){
    var s = document.getElementById('__zb_style');
    if (!s) {
      s = document.createElement('style');
      s.id = '__zb_style';
      (document.head || document.documentElement).appendChild(s);
    }
    s.textContent = CSS;
    if (document.body) document.body.style.paddingTop = '0px';
  }

  function looksBad(el){
    if (!el || el.__zbDone) return;
    var cls = el.className || '';
    if (typeof cls === 'string' && (cls.indexOf('top-header') >= 0 || cls.indexOf('foot-area') >= 0)) {
      el.style.setProperty('display','none','important');
      el.__zbDone = true;
      return;
    }
    var t = (el.innerText || '').trim();
    if (!t || t.length > 30) return;
    for (var j = 0; j < BAD.length; j++){
      if (t.indexOf(BAD[j]) >= 0){
        el.style.setProperty('display','none','important');
        el.__zbDone = true;
        return;
      }
    }
  }

  function sweep(){
    addStyle();
    var all = document.querySelectorAll('.top-header, .foot-area, .tip-fixed, div, section, aside, a, button, span');
    for (var i = 0; i < all.length; i++) looksBad(all[i]);
    var meta = document.querySelector('meta[name="apple-itunes-app"]');
    if (meta && meta.parentNode) meta.parentNode.removeChild(meta);
    if (document.body) {
      document.body.style.paddingTop = '0px';
      document.body.style.marginTop = '0px';
    }
  }

  window.__ZB_SWEEP__ = sweep;

  var pending = false;
  var mo = new MutationObserver(function(muts){
    for (var i = 0; i < muts.length; i++){
      var added = muts[i].addedNodes;
      for (var k = 0; k < added.length; k++){
        if (added[k].nodeType === 1) {
          looksBad(added[k]);
          var kids = added[k].querySelectorAll ? added[k].querySelectorAll('.top-header, .foot-area, a, button') : [];
          for (var j = 0; j < kids.length; j++) looksBad(kids[j]);
        }
      }
    }
    if (pending) return;
    pending = true;
    setTimeout(function(){ pending = false; sweep(); }, 200);
  });

  function boot(){
    sweep();
    try { mo.observe(document.documentElement, {childList:true, subtree:true}); } catch(e){}
    setInterval(function(){
      if (document.visibilityState === 'visible') sweep();
    }, 4000);
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', boot);
  } else { boot(); }
})();
"""

    fun script(isDark: Boolean = true): String {
        val literal = getCss(isDark).trimIndent()
            .replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\r", "")
            .replace("\n", " ")
        return SCRIPT.trimIndent().replace("__CSS__", "'$literal'")
    }
}
