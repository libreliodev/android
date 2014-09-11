(function($, undefined){
  var arraySlice = Array.prototype.slice,
  string = 'string',
  funcStr = 'function';
  // dhtml or (dynamic html) is a series of functions for enabling
  // html contents to have dynamic elements
  var dhtml = function(method)
  {
    var func = methods[method];
    if(!func)
      throw new Error("Undefined function: " + method);
    return func.apply(this, arraySlice.call(arguments, 1));
  },
  methods = {
    list_init: list_init,
    list_new_item: list_new_item,
    list_items_update: list_items_update,
    item_init: item_init,
    item_update: item_init, // deprecated
    eval: function(s, contexts)
    {
      if(!$.isArray(contexts))
        contexts = [ contexts ];
      contexts.push(global_ctx);
      return m_eval(s, contexts, this);
    }
  };

  function list_init()
  {
    // initialize list-items by data-id
    return this.each(function()
      {
        var self = $(this),
        lis = {},
        first_li;
        self.find(' > *').each(function()
          {
            var $this = $(this),
            id = $this.data('id') || 'default';
            if(!first_li)
              first_li = id;
            lis[id] = $this;
            $this.remove();
          });
        self.data('list-items', lis);
        if(!self.data('default-list-item'))
          self.attr('data-default-list-item', first_li);
      });
  }

  var m_parse_keys = {
    '\\': function(ctx, i)
    {
      if(i + 1 == ctx.lbs)
        return;
      ctx.escaped = true;
      ctx.onchar = function(ctx, i, c)
      {
        ctx.onchar = null;
        if(c != '\\')
        {
          ctx.cur += ctx.s[i];
          return false;
        }
      }
    },
    '(': function(ctx, i)
    {
      if(ctx.opren === 0)
        ctx.opren_idx = i;
      ctx.opren++;
      ctx.onchar = function(ctx, i, c)
      {
        if(c === '(' || c === ')')
          return;
        if(i + 1 == ctx.s.length)
          throw new Error("Expected character ')' at end: " + ctx.s);
        return false;
      }
    },
    ')': function(ctx, i)
    {
      var pren = --ctx.opren;
      if(pren === 0)
      {
        var ret = m_parse(ctx.s.substring(ctx.opren_idx + 1, i));
        if(ctx.cur)
          m_parse_var_piece(ctx);
        if(ctx.cur_var)
          ctx.cur_var.call = ret;
        else
          ctx.expr.push(ret);
        ctx.onchar = null;
      }
      else if(pren < 0)
        throw new Error("Unexpected char ')'");
    },
    '.': function(ctx, i)
    {
      if(!ctx.cur)
        throw new Error("Unexpected char '.'");
      m_parse_var_piece(ctx);
    },
    ',': function(ctx, i)
    {
      if(!ctx.cur && ctx.expr.length === 0 && ctx.lcomma_idx != i - 1)
        throw new Error("Unexpected char ','");
      ctx.lcomma_idx = i;
      if(ctx.cur)
        m_parse_var_piece(ctx);
      ctx.cur_var = null;
    },
    '\'': m_parse_char_quote,
    '"': m_parse_char_quote
  };
  var m_parse_char_quote_escape = {
    'n': '\n', 
    'r': '\r',
    't': '\t'
  };
  function m_parse_char_quote(ctx, i, c)
  {
    if(ctx.quote)
    {
      if(ctx.escape)
      {
        ctx.str += c;
        ctx.escape = false;
      }
      else if(c == '\\')
      {
        ctx.escape = !ctx.escape;
      }
      else if(ctx.quote == c)
      {
        ctx.expr.push({
          type: string,
          value: ctx.str
        });
        ctx.onchar = function(ctx, i, c)
        {
          if(c !== ',' && c !== ')')
            throw new Error("Unexpected character '" + c + "'");
          ctx.onchar = null;
        };
        delete ctx.str;
        delete ctx.quote;
        delete ctx.quote_idx;
      }
      else
        ctx.str += c;
    }
    else
    {
      ctx.quote = ctx.s[i];
      ctx.quote_idx = i;
      ctx.onchar = m_parse_char_quote;
      ctx.str = '';
    }
    return false;
  }
  function m_parse_var_piece(ctx)
  {
    var cur = ctx.cur;
    if(ctx.cur_var)
      ctx.cur_var.value.push(cur);
    else
    {
      ctx.cur_var = {
        type: 'var',
        value: [ cur ]
      };
      ctx.expr.push(ctx.cur_var);
    }
    ctx.cur = '';
  }
  function m_parse(s)
  {
    var ctx = {
      s: s,
      cur: '',
      expr: [],
      opren: 0
    };
    for(var i = 0, l = s.length; i < l; ++i)
    {
      var c = s[i];
      if(ctx.onchar)
        if(ctx.onchar(ctx, i, c) === false)
          continue;
      if(m_parse_keys[c])
        m_parse_keys[c](ctx, i, c);
      else
        ctx.cur += c;
    }
    if(ctx.cur)
      m_parse_var_piece(ctx);
    return ctx.expr;
  }
  function m_eval_get_var(ctx, _var)
  {
    var parent;
    for(var i = 0, l = _var.length; i < l; ++i)
    {
      if(ctx === undefined)
      {
        var err = new Error("'" + _var.slice(0, i).join('.') + "' is undefined");
        err.code = 'undefined_var';
        throw err;
      }
      if(i > 0)
        parent = ctx;
      ctx = ctx[_var[i]];
    }
    if(ctx === undefined || ctx === null)
      return;
    return {
      parent: parent,
      value: ctx
    };
  }
  function m_eval(s, contexts, thisarg)
  {
    var exprs = m_parse(s);
    return m_eval_expr(exprs, contexts, thisarg)[0];
  }
  var special_vars = {
    'int': function(expr)
    {
      return parseInt(expr.call[0].value[0]);
    },
    'float': function(expr)
    {
      return parseFloat(expr.call[0].value[0].join('.'));
    },
    'if': function(expr, contexts, thisarg)
    {
      var call = expr.call;
      if(!call)
        throw new Error('if statement cannot be used as a variable');
      
      if(m_eval_expr([ expr.call[0] ], contexts, thisarg)[0])
      {
        if(expr.call[1])
          return m_eval_expr([ expr.call[1] ], contexts, thisarg)[0];
      }
      else
      {
        if(expr.call[2])
          return m_eval_expr([ expr.call[1] ], contexts, thisarg)[0];
      }
    }
  };
  function m_eval_expr(exprs, contexts, thisarg)
  {
    var ret = [];
    if(!$.isArray(contexts))
      contexts = [ contexts ];
    for(var i = 0, l = exprs.length; i < l; ++i)
    {
      var expr = exprs[i];
      switch(expr.type)
      {
      case 'var':
        var val = undefined;
        if(special_vars[expr.value[0]])
        {
          ret.push(special_vars[expr.value[0]](expr, contexts, thisarg));
          break;
        }
        if(expr.value[0] == 'this')
        {
          if(expr.value.length > 1)
            val = m_eval_get_var(thisarg, expr.value.slice(1));
          else
            val = thisarg;
        }
        for(var c = 0, cl = contexts.length; val === undefined && c < cl; ++c)
        {
          try {
            val = m_eval_get_var(contexts[c], expr.value);
          }catch(e) {
            if(e.code != 'undefined_var' || c + 1 == cl)
              throw e;
          }
        }
        if(expr.call)
        {
          if(!val)
            throw new Error("'" + expr.value.join('.') + 
                            "' is undefined");
          if(typeof val.value != funcStr)
            throw new Error("'" + expr.value.join('.') + 
                            "' is not a function");
          var parent = val.parent;
          val = val.value;
          ret.push(val.apply(parent || thisarg, 
                             m_eval_expr(expr.call, contexts, thisarg)));
        }
        else
          ret.push(val ? val.value : val);
        break;
      case string:
        ret.push(expr.value);
        break;
      }
    }
    return ret;
  }
  var global_ctx = {
    "false": false,
    "true": true,
    replace: function(a, b)
    {
      this.html(this.html().replace(a, b));
    },
    html: function(a)
    {
      this.html(a);
    },
    mul: function(a, b)
    {
      return parseFloat(a) * parseFloat(b);
    },
    add: function(a, b)
    {
      return parseFloat(a) + parseFloat(b);
    },
    mod_style: function()
    {
      var args = arguments;
      for(var i = 0, l = args.length; i < l; i += 2)
        this.css(args[i], args[i+1]||'');
    },
    get: function(a, p)
    {
      return a[p];
    },
    Object: function()
    {
      var ret = {};
      for(var i = 0; i < arguments.length; i += 2)
        ret[arguments[i]] = arguments[i + 1];
      return ret;
    },
    concat: function()
    {
      if($.isArray(arguments[0]))
        return Array.prototype.concat(arguments[0], 
                                      arraySlice.call(arguments, 1))
      var ret = '';
      for(var i = 0; i < arguments.length; ++i)
        ret += arguments[i];
      return ret;
    },
    // logical functions
    not: function(a) { return !a; },
    eqci: function(a, b) { return (a+'').toLowerCase() == (b+'').toLowerCase(); },
    eq: function(a, b) { return (a+'') == (b+''); },
    eeq: function(a, b) { return a == b; },
    eeeq: function(a, b) { return a === b; },
    gt: function(a, b) { return parseFloat(a) > parseFloat(b); },
    gte: function(a, b) { return parseFloat(a) <= parseFloat(b); },
    lt: function(a, b) { return parseFloat(a) < parseFloat(b); },
    lte: function(a, b) { return parseFloat(a) <= parseFloat(b); }
  };
  function list_new_item(id, contexts)
  {
    var self = this,
    lis = self.data('list-items');
    id = id || self.data('default-list-item');
    if(!lis[id])
      return null;
    var li = lis[id].clone();
    if(contexts)
      list_items_update.call(li, contexts);
    return li;
  }
  function list_items_update(contexts)
  {
    this.dhtml('item_init', contexts, {
      recursive: true
    });
  }
  function eq_list(s, cb)
  {
    var akeys = s.split(';');
    $.each(akeys, function(i, k)
      {
        var idx = k.indexOf('='),
        key = idx == -1 ? k : k.substr(0, idx),
        val = idx == -1 ? k : k.substr(idx + 1);
        if(!key || !val)
          return;
        cb(key, val);
      });
  }
  function item_init(contexts, opts)
  {
    function foreach(a, eachp)
    {
      if($.isArray(a))
      {
        for(var i = 0; i < a.length; ++i)
          eachp(a[i], i);
      }
      else
      {
        for(var i in a)
          eachp(a[i], i);
      }
    }
    opts = opts || {};
    if(!$.isArray(contexts))
      contexts = [ contexts ];
    contexts.push(global_ctx);
    //DEPRECATED:: onupdate attribute is deprecated
    var expr_vars =  ['init', 'onupdate', 'content', 'content-html'],
    list_expr_vars = ['attrs', 'bind'],
    foreach_cache_get = opts.foreach_cache_get,
    foreach_cache_set = opts.foreach_cache_set,
    exprs = parse_exprs(this);
    clean(this);
    item_init_subrout(this, exprs, contexts, opts, {
      foreach_counter: 0
    });
    function clean($els)
    {
      var total_vars = ([ 'foreach' ]).concat(expr_vars, list_expr_vars),
      children;
      if(opts.recursive)
        children = $els.find('*');
      for(var i = 0; i < total_vars.length; ++i)
      {
        var attr = 'data-' + total_vars[i];
        $els.removeAttr(attr);
        if(opts.recursive)
          children.removeAttr(attr);
      }
    }
    function parse_exprs($els)
    {
      var ret = [];
      for(var x = 0, xl = $els.length; x < xl; ++x)
      {
        var $el = $els.eq(x),
        exprs = {};
        
        for(var i = 0, l = expr_vars.length; i < l; ++i)
        {
          var name = expr_vars[i],
          val = $el.data(name) || '';
          exprs[name] = m_parse(val);
          exprs[name].value = val;
        }
        
        for(var i = 0, l = list_expr_vars.length; i < l; ++i)
        {
          var name = list_expr_vars[i],
          tmp = exprs[name] = {};
          eq_list($el.data(name) || '', function(key, val_str)
            {
              tmp[key] = m_parse(val_str);
              tmp[key].value = val_str;
            });
        }
        
        var val = $el.data('foreach');
        if(val)
        {
          var tmp = val.split(' as ');
          if(tmp.length != 2)
            throw new Error("foreach value is not valid: " + val);
          var each = tmp[1].split('=>'),
          key_var, value_var;
          if(each.length == 1)
            value_var = each[0];
          else if(each.length == 2 && each[1])
          {
            key_var = each[0];
            value_var = each[1];
          }
          else
            throw new Error("foreach variables are not valid: " + tmp[1]);
          exprs.foreach = {
            forexpr: m_parse(tmp[0]),
            key_var: key_var,
            value_var: value_var
          };
          exprs.foreach.forexpr.value = tmp[0];
        }
        
        exprs.childNodes = parse_exprs($el.find(' > *'));
        ret.push(exprs);
      }
      return ret;
    }
    function item_init_subrout($els, $els_exprs, contexts, stat)
    {
      function bind_expr($el, key, expr)
      {
        $el.bind(key, function()
          {
            var res = m_eval_expr(expr, contexts, $el);
            return res[res.length - 1];
          });
      }
      for(var x = 0, xl = $els.length; x < xl; ++x)
      {
        var $el = $els.eq(x),
        exprs = $els_exprs[x],
        tmp;
        // eval for | it will be applied to its children
        if(exprs.foreach)
        {
          var foreach_expr = exprs.foreach,
          key_var = foreach_expr.key_var, 
          value_var = foreach_expr.value_var,
          forexpr = foreach_expr.forexpr,
          exprsi = $.extend(false, {}, exprs);
          delete exprsi.foreach;
          exprsi = [exprsi];

          tmp = m_eval_expr(forexpr, contexts, $el)[0];
          if(typeof tmp == 'object')
          {
            foreach(tmp, function(value, key)
              {
                var cache_v;
                if(foreach_cache_get && 
                   (cache_v = foreach_cache_get(forexpr, key, value, 
                                                stat.foreach_counter)))
                {
                  $(cache_v).insertBefore($el);
                  return;
                }
                var $nel = $el.clone(),
                sctxs = contexts.concat(),
                sctx = {};
                sctxs.unshift(sctx);
                if(key_var)
                  sctx[key_var] = key;
                if(value_var)
                  sctx[value_var] = value;
                $nel.insertBefore($el);
                item_init_subrout($nel, exprsi, sctxs, stat);
                if(foreach_cache_set)
                  foreach_cache_set(forexpr, key, value, 
                                    stat.foreach_counter, $nel[0]);
              });
            
            stat.foreach_counter++;
            $el.remove();
            return;
          }
          else
            throw new Error("foreach expression returns unexpected value: " +
                            "expr: `" + forexpr.value + "` is " + typeof tmp);
        }
      
        for(var i = 0; i < expr_vars.length; ++i)
        {
          var expr_var = expr_vars[i];
          if(exprs[expr_var])
          {
            tmp = m_eval_expr(exprs[expr_var], contexts, $el)[0];
            switch(expr_var)
            {
            case 'content':
              if(tmp || tmp === '')
                $el.text(tmp+'');
              break;
            case 'content-html':
              if(tmp || tmp === '')
                $el.html(tmp+'');
              break;
            case 'onupdate':
            case 'init':
              break;
            }
          }
        }

        for(var key in exprs.attrs)
        {
          tmp = m_eval_expr(exprs.attrs[key], contexts, $el)[0];
          if(tmp !== null && tmp !== undefined && tmp !== '')
            $el.attr(key, tmp+'');
          else
            $el.removeAttr(key);
        }
        
        for(var key in exprs.bind)
        {
          var expr = exprs.bind[key];
          if(expr.length == 1 && expr[0].type == 'var' && !expr[0].call)
          {
            tmp = m_eval_expr(expr, contexts, $el)[0];
            if(typeof tmp != 'function')
              throw new Error("Couldn't bind on " + key + ", `" + val_str + 
                              "` is not a function");
            $el.bind(key, tmp);
          }
          else
            bind_expr($el, key, expr)
        }
        if(opts.recursive && exprs.childNodes && exprs.childNodes.length > 0)
          item_init_subrout($el.find(' > *'), exprs.childNodes, contexts, stat);
      }
    }
  }
  $.fn.dhtml = dhtml;
})(jQuery);
