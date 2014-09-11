(function(window, translate){
  var content_key = '_localize_content',
  attr_key_pref = '_localize_attr_';
  var localize = function(a)
  {
    return translate((a+'').trim());
  };
  function localize_content(v)
  {
    var $el = this;
    if(!v)
    {
      v = $el.data(content_key);
      if(!v)
        $el.data(content_key, v = $el.text()); // store original content
    }
    else
      $el.data(content_key, v);
    $el.text(localize(v));
  }
  function localize_attr(a, v)
  {
    var $el = this,
    key = attr_key_pref + a;
    if(v === undefined)
    {
      v = $el.data(key);
      if(!v)
        $el.data(key, v = $el.attr(a));
    }
    else
      $el.data(key, v);
    $el.attr(a, localize(v));
  }
  localize.content = localize_content;
  localize.attr = localize_attr;
  var localize_ctx = {
    _: localize
  };
  function localize_eval_all(el)
  {
    $('*', el).each(function()
      {
        var val = this.getAttribute('localize');
        if(typeof val != 'string')
          return;
        var $this = $(this);
        if(val == '')
          localize_content.call($this);
        else
        {
          try {
            var ret = $this.dhtml('eval', val, localize_ctx);
            if(typeof ret == 'string')
              $this.text(localize(ret));
          } catch(e) {
            $this.text(localize(val));
          }
        }
      });
  }
  localize.eval_all = localize_eval_all;
  localize.setLocale = function(locale, opts, cb)
  {
    if(typeof opts == 'function')
      cb = opts;
    if(opts.icu === undefined || opts.icu)
    {
      var icu = document.createElement('script');
      icu.src = assets + '/lib/localeplanet/icu_' + locale + '.js';
      $('body').append(icu);
      localize.locale = locale;
      icu.onload = function()
      {
        icu.parentNode.removeChild(icu);
      }
    }
    var trans_path = assets + '/lang/' + locale + 
       (localize.file ? '/' + localize.file : '') + '.json';
    $.ajax(trans_path, $.extend({
      dataType: 'json',
      success: function(res)
      {
        _.setTranslation(res);
        if(opts.update === undefined || opts.update)
            localize_eval_all();
        cb && cb();
      },
      error: function(xhr, err_text)
      {
        console.log('Request to `' + trans_path + '` has failed! ' + err_text);
        cb && cb(err_text)
      }
    }, opts))
  }
  window.localize = localize;
  // setlocale to default librelio locale
  var navLang = navigator && navigator.language ? navigator.language : null;
  localize.setLocale(navLang || config.locale,
                     {async:false,icu:false,update:false}, function(err)
    {
      if(err && navLang && navLang != config.locale)
        localize.setLocale(config.locale, {async:false,icu:false,update:false});
    });
  $(function(){ localize.eval_all(); });
})(window, translate || _);
