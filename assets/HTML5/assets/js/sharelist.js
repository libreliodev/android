(function(window){
function parse_url(url_str, listobj)
{
  var query = querystring.parse(url('?', url_str));
  return {
    image: url('path', url_str),
    sites: (query.wasites||'').split(','),
    url: query.waurl || '',
    title: query.watitle || '',
    text: query.watext || ''
  };
}
var template,
sharelist = {
  setTemplate: function(tmpl_el)
  {
    template = $(tmpl_el);
    template.dhtml('list_init');
    if(tmpl_el.parentNode)
      tmpl_el.parentNode.removeChild(tmpl_el);
  },
  new: function(url_str)
  {
    var $el = template.clone().show(),
    listobj = {
      element: $el[0],
      $element: $el,
      url: url_str
    },
    info = listobj.info = parse_url(url_str),
    ctx = {
      share_mail_href: function()
      {
        var info = listobj.info;
        return 'mailto:?' + querystring.stringify({
          subject: info.title,
          body: info.text + ' ' + info.url
        });
      },
      share_facebook_clicked: function(e)
      {
        e.preventDefault();
        FB.ui(
          {
            method: 'share',
            href: info.url,
          },
          function(response)
          {
            if(response && response.error_message)
            {
              alert(info.url + ': ' + response.error_message);
            }
          }
        );
        return false;
      },
      share_twitter_clicked: function(e)
      {
        var params = 'height=450, width=550, top=' + 
          ($(window).height()/2 - 225) + ', left=' +
          $(window).width()/2 + ', toolbar=0,' +
          ' location=0, menubar=0, directories=0, scrollbars=0';
        e.preventDefault();
        //We trigger a new window with the Twitter dialog, in the middle of the page
        window.open('http://twitter.com/share?' + querystring.stringify({
          url: info.url,
          text: info.title + '\n' + info.text
        }), 'twitterwindow', params);
        return false;
      },
      info: info
    },
    sites = info.sites;
    for(var i = 0; i < sites.length; ++i)
    {
      try {
        var site = sites[i],
        item = template.dhtml('list_new_item', site);
      } catch(e) {
        console.log('share not supported for ' + site);
      } finally {
        $el.append(item);
        item.dhtml('item_init', ctx, { recursive: true });
      }
    }
    return listobj;
  },
  isSharelist: function(url_str)
  {
    return url_protocol(url_str) == 'share:';
  }
};

window.sharelist = sharelist;
})(window);
