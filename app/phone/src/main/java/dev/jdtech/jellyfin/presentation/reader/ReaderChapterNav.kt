package dev.jdtech.jellyfin.presentation.reader

import android.webkit.JavascriptInterface

/** Bridge used by the chapter navigation injected into Readium's HTML resource. */
class ChapterNavBridge(
    private val canPrevious: Boolean,
    private val canNext: Boolean,
    private val onPrevious: () -> Unit,
    private val onNext: () -> Unit,
) {
    @JavascriptInterface fun canGoPrevious(): Boolean = canPrevious

    @JavascriptInterface fun canGoNext(): Boolean = canNext

    @JavascriptInterface
    fun goPrevious() {
        onPrevious()
    }

    @JavascriptInterface
    fun goNext() {
        onNext()
    }
}

/**
 * Injected into the current reflowable Readium resource in scroll mode.
 *
 * The header/footer are real elements in the chapter, so Readium's native scrolling moves the text
 * itself and reveals the chapter control below/above it. The ring is drawn with an SVG stroke; the
 * icon is a Lucide chevron.
 */
internal const val CHAPTER_NAV_SCRIPT =
    """
    (function() {
      var bridge = window.FindroidChapterNav;
      if (!bridge) return;
      if (document.getElementById('findroid-chapter-nav-prev') ||
          document.getElementById('findroid-chapter-nav-next')) {
        if (window.__findroidChapterNavUpdate) window.__findroidChapterNavUpdate();
        return;
      }

      var style = document.getElementById('findroid-chapter-nav-style');
      if (!style) {
        style = document.createElement('style');
        style.id = 'findroid-chapter-nav-style';
        style.textContent =
          '.findroid-chapter-nav{' +
            'display:flex!important;align-items:center!important;justify-content:center!important;' +
            'width:100%!important;height:96px!important;min-height:96px!important;' +
            'margin:0!important;padding:0!important;box-sizing:border-box!important;' +
            'line-height:0!important;' +
          '}' +
          '.findroid-chapter-nav-circle{' +
            'position:relative!important;width:48px!important;height:48px!important;' +
            'opacity:0.35;will-change:opacity,transform;' +
          '}' +
          '.findroid-chapter-nav-ring{' +
            'position:absolute!important;inset:0!important;width:48px!important;height:48px!important;' +
            'overflow:visible!important;' +
          '}' +
          '.findroid-chapter-nav-fill{' +
            'fill:rgba(0,0,0,.85)!important;stroke:none!important;' +
          '}' +
          '.findroid-chapter-nav-track{' +
            'fill:none!important;stroke:rgba(255,255,255,.22)!important;stroke-width:3!important;' +
          '}' +
          '.findroid-chapter-nav-progress{' +
            'fill:none!important;stroke:#fff!important;stroke-width:3!important;' +
            'stroke-linecap:round!important;stroke-dasharray:131.95;stroke-dashoffset:131.95;' +
          '}' +
          '.findroid-chapter-nav-icon{' +
            'position:absolute!important;left:12px!important;top:12px!important;' +
            'width:24px!important;height:24px!important;fill:none!important;' +
            'stroke:#fff!important;stroke-width:2!important;stroke-linecap:round!important;' +
            'stroke-linejoin:round!important;' +
          '}';
        (document.head || document.documentElement).appendChild(style);
      }

      var canPrev = false;
      var canNext = false;
      try { canPrev = !!bridge.canGoPrevious(); } catch (error) {}
      try { canNext = !!bridge.canGoNext(); } catch (error) {}

      var circumference = 2 * Math.PI * 21;

      function makeNav(kind, iconPath) {
        var nav = document.createElement('div');
        nav.className = 'findroid-chapter-nav';
        nav.id = 'findroid-chapter-nav-' + kind;
        nav.setAttribute('aria-hidden', 'true');
        nav.innerHTML =
          '<div class="findroid-chapter-nav-circle">' +
            '<svg class="findroid-chapter-nav-ring" viewBox="0 0 48 48">' +
              '<circle class="findroid-chapter-nav-fill" cx="24" cy="24" r="21"></circle>' +
              '<circle class="findroid-chapter-nav-track" cx="24" cy="24" r="21"></circle>' +
              '<circle class="findroid-chapter-nav-progress" cx="24" cy="24" r="21" ' +
                'transform="rotate(-90 24 24)"></circle>' +
            '</svg>' +
            '<svg class="findroid-chapter-nav-icon" viewBox="0 0 24 24">' +
              '<path d="' + iconPath + '"></path>' +
            '</svg>' +
          '</div>';
        return nav;
      }

      var previous = canPrev ? makeNav('prev', 'm18 15-6-6-6 6') : null;
      var next = canNext ? makeNav('next', 'm6 9 6 6 6-6') : null;
      if (previous) document.body.insertBefore(previous, document.body.firstChild);
      if (next) document.body.appendChild(next);

      if (previous && window.scrollY <= 1) {
        var headerRect = previous.getBoundingClientRect();
        var headerBottom = headerRect.bottom + (window.scrollY || 0);
        if (headerBottom > 0) window.scrollTo(0, headerBottom);
      }

      function setProgress(nav, progress) {
        if (!nav) return;
        var circle = nav.querySelector('.findroid-chapter-nav-circle');
        var progressCircle = nav.querySelector('.findroid-chapter-nav-progress');
        if (circle) {
          circle.style.opacity = (0.35 + 0.65 * progress).toFixed(2);
          circle.style.transform = 'scale(' + (0.86 + 0.14 * progress).toFixed(3) + ')';
        }
        if (progressCircle) {
          progressCircle.style.strokeDasharray = circumference.toFixed(2);
          progressCircle.style.strokeDashoffset = (circumference * (1 - progress)).toFixed(2);
        }
      }

      function headerProgress() {
        if (!previous) return 0;
        var height = Math.max(1, previous.offsetHeight);
        var rect = previous.getBoundingClientRect();
        return Math.max(0, Math.min(1, 1 + rect.top / height));
      }

      function footerProgress() {
        if (!next) return 0;
        var height = Math.max(1, next.offsetHeight);
        var rect = next.getBoundingClientRect();
        return Math.max(0, Math.min(1, (window.innerHeight - rect.top) / height));
      }

      function update() {
        setProgress(previous, headerProgress());
        setProgress(next, footerProgress());
      }

      window.__findroidChapterNavUpdate = update;
      update();

      var overscrollStart = null;
      var touchStartY = null;
      var armedPrevious = false;
      var armedNext = false;

      window.addEventListener('scroll', update, { passive: true });
      window.addEventListener('resize', update);
      window.addEventListener('touchstart', function(event) {
        touchStartY = event.touches && event.touches.length === 1
          ? event.touches[0].clientY
          : null;
        overscrollStart = null;
        armedPrevious = false;
        armedNext = false;
      }, { passive: true });
      window.addEventListener('touchmove', function(event) {
        if (!event.touches || event.touches.length !== 1 || touchStartY === null) return;
        var y = event.touches[0].clientY;
        var gesture = y - touchStartY;
        if (gesture < -8 && next && footerProgress() >= 0.98) {
          if (overscrollStart === null) overscrollStart = y;
          if (overscrollStart - y > 24) armedNext = true;
        } else if (gesture > 8 && previous && headerProgress() >= 0.98) {
          if (overscrollStart === null) overscrollStart = y;
          if (y - overscrollStart > 24) armedPrevious = true;
        } else {
          overscrollStart = null;
        }
      }, { passive: true });
      window.addEventListener('touchend', function() {
        var goPrevious = armedPrevious;
        var goNext = armedNext;
        armedPrevious = false;
        armedNext = false;
        overscrollStart = null;
        touchStartY = null;
        if (goPrevious) {
          try { bridge.goPrevious(); } catch (error) {}
        } else if (goNext) {
          try { bridge.goNext(); } catch (error) {}
        }
      }, { passive: true });
    })();
    """

internal const val CHAPTER_NAV_REMOVE_SCRIPT =
    """
    (function() {
      var ids = ['findroid-chapter-nav-prev', 'findroid-chapter-nav-next', 'findroid-chapter-nav-style'];
      for (var i = 0; i < ids.length; i++) {
        var element = document.getElementById(ids[i]);
        if (element && element.parentNode) element.parentNode.removeChild(element);
      }
      window.__findroidChapterNavUpdate = null;
    })();
    """
