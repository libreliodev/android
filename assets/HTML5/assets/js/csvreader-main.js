$(function(){
  setTimeout(function()
    {
      var doc_query = querystring.parse(get_url_query(document.location+'')),
      csv_url = doc_query ? doc_query.waurl : null;
      if(!csv_url)
        return;
      var tmpl_url = csvreader_template_url(csv_url);
      csv_url = url4webview(csv_url);
      tmpl_url = url4webview(tmpl_url);
      init_csvreader(csv_url, tmpl_url);
    });
});
