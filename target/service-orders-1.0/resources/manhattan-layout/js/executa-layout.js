/**
 * Executa — Manhattan layout (produção, sem showcase)
 */
var ExecutaLayout = {

    init: function(cfg) {
        this.cfg = cfg || {};
        this.wrapper = $(document.body).children('.layout-wrapper');
        this.contentWrapper = this.wrapper.children('.layout-main');
        this.sidebar = this.wrapper.children('.layout-sidebar');
        this.menu = this.sidebar.find('.layout-menu');
        this.jq = this.menu;
        this.menulinks = this.menu.find('a');
        this.topbar = this.contentWrapper.children('.layout-topbar');
        this.menuContainer = this.sidebar.find('.layout-menu-container');
        this.menuButton = $('#layout-menu-btn');
        this.topbarMenu = this.topbar.find('> .layout-topbar-menu-wrapper > .topbar-menu');
        this.topbarItems = this.topbarMenu.children('li');
        this.topbarLinks = this.topbarMenu.find('a');
        this.anchorButton = $('#layout-sidebar-anchor');

        this.bindEvents();

        if (!this.isHorizontal()) {
            this.restoreMenuState();
        }

        this.expandedMenuitems = this.expandedMenuitems || [];
    },

    bindEvents: function() {
        var $this = this;

        this.sidebar.off('mouseenter.sidebar mouseleave.sidebar click.sidebar').on('mouseenter.sidebar', function() {
            if ($this.isSlim()) {
                if ($this.hideTimeout) {
                    clearTimeout($this.hideTimeout);
                }
                $this.wrapper.addClass('layout-slim-active');
            }
        }).on('mouseleave.sidebar', function() {
            if ($this.wrapper.hasClass('layout-slim-active')) {
                $this.hideTimeout = setTimeout(function() {
                    $this.wrapper.removeClass('layout-slim-active');
                }, 250);
            }
        }).on('click.sidebar', function() {
            $this.sidebarClick = true;
        });

        $this.menu.off('click.menu').on('click.menu', function() {
            $this.menuClick = true;
        });

        $this.menulinks.off('click.menu').on('click.menu', function(e) {
            var link = $(this),
                item = link.parent('li'),
                submenu = item.children('ul');

            if (item.hasClass('active-menuitem')) {
                if (submenu.length) {
                    $this.removeMenuitem(item.attr('id'));
                    item.removeClass('active-menuitem');

                    if ($this.isHorizontal()) {
                        submenu.hide();
                    } else {
                        submenu.slideUp();
                    }
                }

                if (item.parent().is($this.jq)) {
                    $this.menuActive = false;
                }
            } else {
                $this.addMenuitem(item.attr('id'));

                if ($this.isHorizontal()) {
                    $this.deactivateItems(item.siblings(), false);

                    if (submenu.length === 0) {
                        $this.resetMenu();
                    }
                } else {
                    $this.deactivateItems(item.siblings(), true);
                    $.cookie('manhattan_menu_scroll_state', link.attr('href') + ',' + $this.menuContainer.scrollTop(), { path: '/' });
                }

                $this.activate(item);

                if (item.parent().is($this.jq)) {
                    $this.menuActive = true;
                }
            }

            if (submenu.length) {
                e.preventDefault();
            }
        });

        $this.menu.find('> li').off('mouseenter.menu').on('mouseenter.menu', function() {
            if ($this.isHorizontal()) {
                var item = $(this);

                if (!item.hasClass('active-menuitem')) {
                    $this.menu.find('.active-menuitem').removeClass('active-menuitem');
                    $this.menu.find('ul:visible').hide();

                    if ($this.menuActive) {
                        item.addClass('active-menuitem');
                        item.children('ul').show();
                    }
                }
            }
        });

        this.topbarLinks.off('click.topbar').on('click.topbar', function(e) {
            var link = $(this),
                href = link.attr('href');

            $this.topbarClick = true;

            if (href && href !== '#') {
                window.location.href = href;
            }

            e.preventDefault();
        });

        this.anchorButton.off('click.menu').on('click.menu', function(e) {
            $this.wrapper.removeClass('layout-slim-restore');
            $this.wrapper.toggleClass('layout-slim-anchored');
            $this.saveMenuState();

            setTimeout(function() {
                $(window).trigger('resize');
            }, 200);

            e.preventDefault();
        });

        this.menuButton.off('click.menu').on('click.menu', function(e) {
            $this.menuClick = true;

            if ($this.isMobile()) {
                $this.wrapper.toggleClass('layout-mobile-active');
            } else {
                if ($this.isStatic()) {
                    $this.wrapper.toggleClass('layout-static-inactive');
                } else if ($this.isOverlay()) {
                    $this.wrapper.toggleClass('layout-overlay-active');
                } else if ($this.isToggle()) {
                    $this.wrapper.toggleClass('layout-toggle-active');
                }
            }

            setTimeout(function() {
                $(window).trigger('resize');
            }, 350);

            e.preventDefault();
        });

        this.contentWrapper.children('.layout-main-mask').off('click.mask').on('click.mask', function() {
            $this.wrapper.removeClass('layout-mobile-active layout-slim-restore');
            $(document.body).removeClass('hidden-overflow');
        });

        $(document.body).off('click.layoutBody').on('click.layoutBody', function() {
            if (!$this.menuClick && $this.isHorizontal()) {
                $this.menu.find('.active-menuitem').removeClass('active-menuitem');
                $this.menu.find('ul:visible').hide();
                $this.menuActive = false;
            }

            if (!$this.menuClick && !$this.sidebarClick) {
                $this.wrapper.removeClass('layout-overlay-active layout-toggle-active');
            }

            if (!$this.topbarClick) {
                $this.topbarItems.filter('.active-topmenuitem').removeClass('active-topmenuitem');
                $this.topbarMenu.removeClass('topbar-menu-active');
            }

            $this.sidebarClick = false;
            $this.menuClick = false;
            $this.topbarClick = false;
        });
    },

    activate: function(item) {
        var submenu = item.children('ul');
        item.addClass('active-menuitem');

        if (submenu.length) {
            if (this.isHorizontal()) {
                submenu.show();
            } else {
                submenu.slideDown();
            }
        }
    },

    deactivate: function(item) {
        var submenu = item.children('ul');
        item.removeClass('active-menuitem');

        if (submenu.length) {
            submenu.hide();
        }
    },

    deactivateItems: function(items, animate) {
        var $this = this;

        for (var i = 0; i < items.length; i++) {
            var item = items.eq(i),
                submenu = item.children('ul');

            if (submenu.length) {
                if (item.hasClass('active-menuitem')) {
                    var activeSubItems = item.find('.active-menuitem');
                    item.removeClass('active-menuitem');

                    if (animate) {
                        submenu.slideUp('normal', function() {
                            $(this).parent().find('.active-menuitem').each(function() {
                                $this.deactivate($(this));
                            });
                        });
                    } else {
                        submenu.hide();
                        item.find('.active-menuitem').each(function() {
                            $this.deactivate($(this));
                        });
                    }

                    $this.removeMenuitem(item.attr('id'));
                    activeSubItems.each(function() {
                        $this.removeMenuitem($(this).attr('id'));
                    });
                } else {
                    item.find('.active-menuitem').each(function() {
                        var subItem = $(this);
                        $this.deactivate(subItem);
                        $this.removeMenuitem(subItem.attr('id'));
                    });
                }
            } else if (item.hasClass('active-menuitem')) {
                $this.deactivate(item);
                $this.removeMenuitem(item.attr('id'));
            }
        }
    },

    removeMenuitem: function(id) {
        this.expandedMenuitems = $.grep(this.expandedMenuitems, function(value) {
            return value !== id;
        });
        this.saveMenuState();
    },

    addMenuitem: function(id) {
        if ($.inArray(id, this.expandedMenuitems) === -1) {
            this.expandedMenuitems.push(id);
        }
        this.saveMenuState();
    },

    saveMenuState: function() {
        if (this.wrapper.hasClass('layout-slim-anchored')) {
            $.cookie('manhattan_slim_menu_anchored', 'manhattan_slim_menu_anchored', { path: '/' });
        } else {
            $.removeCookie('manhattan_slim_menu_anchored', { path: '/' });
        }

        $.cookie('manhattan_expandeditems', this.expandedMenuitems.join(','), { path: '/' });
    },

    clearMenuState: function() {
        $.removeCookie('manhattan_expandeditems', { path: '/' });
        $.removeCookie('manhattan_slim_menu_anchored', { path: '/' });
    },

    restoreMenuState: function() {
        var $this = this;
        var menuCookie = $.cookie('manhattan_expandeditems');

        if (menuCookie) {
            this.expandedMenuitems = menuCookie.split(',');
            for (var i = 0; i < this.expandedMenuitems.length; i++) {
                var id = this.expandedMenuitems[i];
                if (id) {
                    var menuitem = $('#' + this.expandedMenuitems[i].replace(/:/g, '\\:'));
                    menuitem.addClass('active-menuitem');

                    var submenu = menuitem.children('ul');
                    if (submenu.length) {
                        submenu.show();
                    }
                }
            }

            setTimeout(function() {
                $this.restoreScrollState(menuitem);
            }, 100);
        }

        var sidebarCookie = $.cookie('manhattan_slim_menu_anchored');
        if (sidebarCookie) {
            this.wrapper.addClass('layout-slim-anchored layout-slim-restore');
        }
    },

    restoreScrollState: function(menuitem) {
        var scrollState = $.cookie('manhattan_menu_scroll_state');
        if (scrollState) {
            var state = scrollState.split(',');
            if (state[0].startsWith(this.cfg.pathname) || this.isScrolledIntoView(menuitem, state[1])) {
                this.menuContainer.scrollTop(parseInt(state[1], 10));
            } else {
                this.scrollIntoView(menuitem.get(0));
                $.removeCookie('manhattan_menu_scroll_state', { path: '/' });
            }
        } else if (!this.isScrolledIntoView(menuitem, menuitem.scrollTop())) {
            this.scrollIntoView(menuitem.get(0));
        }
    },

    scrollIntoView: function(elem) {
        if (document.documentElement.scrollIntoView) {
            elem.scrollIntoView({ block: 'nearest', inline: 'start' });

            var container = $('.layout-menu-container');
            var scrollTop = container.scrollTop();
            if (scrollTop > 0) {
                container.scrollTop(scrollTop + parseFloat(this.topbar.height()));
            }
        }
    },

    isScrolledIntoView: function(elem, scrollTop) {
        var viewBottom = parseInt(scrollTop, 10) + this.menuContainer.height();
        var elemTop = elem.position().top;
        var elemBottom = elemTop + elem.height();

        return ((elemBottom <= viewBottom) && (elemTop >= scrollTop));
    },

    isSlim: function() {
        return this.wrapper.hasClass('layout-slim') && this.isDesktop();
    },

    isStatic: function() {
        return this.wrapper.hasClass('layout-static') && this.isDesktop();
    },

    isHorizontal: function() {
        return this.wrapper.hasClass('layout-horizontal') && this.isDesktop();
    },

    isOverlay: function() {
        return this.wrapper.hasClass('layout-overlay') && this.isDesktop();
    },

    isToggle: function() {
        return this.wrapper.hasClass('layout-toggle') && this.isDesktop();
    },

    isDesktop: function() {
        return window.innerWidth > 1280;
    },

    isMobile: function() {
        return window.innerWidth <= 1280;
    },

    resetMenu: function() {
        this.menu.find('.active-menuitem').removeClass('active-menuitem');
        this.menu.find('ul:visible').hide();
        this.menuActive = false;
    }
};

(function(factory) {
    if (typeof define === 'function' && define.amd) {
        define(['jquery'], factory);
    } else if (typeof exports === 'object') {
        module.exports = factory(require('jquery'));
    } else {
        factory(jQuery);
    }
}(function($) {
    var pluses = /\+/g;

    function encode(s) {
        return config.raw ? s : encodeURIComponent(s);
    }

    function decode(s) {
        return config.raw ? s : decodeURIComponent(s);
    }

    function stringifyCookieValue(value) {
        return encode(config.json ? JSON.stringify(value) : String(value));
    }

    function parseCookieValue(s) {
        if (s.indexOf('"') === 0) {
            s = s.slice(1, -1).replace(/\\"/g, '"').replace(/\\\\/g, '\\');
        }

        try {
            s = decodeURIComponent(s.replace(pluses, ' '));
            return config.json ? JSON.parse(s) : s;
        } catch (e) { }
    }

    function read(s, converter) {
        var value = config.raw ? s : parseCookieValue(s);
        return $.isFunction(converter) ? converter(value) : value;
    }

    var config = $.cookie = function(key, value, options) {
        if (arguments.length > 1 && !$.isFunction(value)) {
            options = $.extend({}, config.defaults, options);

            if (typeof options.expires === 'number') {
                var days = options.expires, t = options.expires = new Date();
                t.setMilliseconds(t.getMilliseconds() + days * 864e+5);
            }

            return (document.cookie = [
                encode(key), '=', stringifyCookieValue(value),
                options.expires ? '; expires=' + options.expires.toUTCString() : '',
                options.path ? '; path=' + options.path : '',
                options.domain ? '; domain=' + options.domain : '',
                options.secure ? '; secure' : ''
            ].join(''));
        }

        var result = key ? undefined : {},
            cookies = document.cookie ? document.cookie.split('; ') : [],
            i = 0,
            l = cookies.length;

        for (; i < l; i++) {
            var parts = cookies[i].split('='),
                name = decode(parts.shift()),
                cookie = parts.join('=');

            if (key === name) {
                result = read(cookie, value);
                break;
            }

            if (!key && (cookie = read(cookie)) !== undefined) {
                result[name] = cookie;
            }
        }

        return result;
    };

    config.defaults = {};

    $.removeCookie = function(key, options) {
        $.cookie(key, '', $.extend({}, options, { expires: -1 }));
        return !$.cookie(key);
    };
}));

if (window.PrimeFaces && window.PrimeFaces.widget.Dialog) {
    PrimeFaces.widget.Dialog = PrimeFaces.widget.Dialog.extend({
        enableModality: function() {
            this._super();
            $(document.body).children(this.jqId + '_modal').addClass('ui-dialog-mask');
        },
        syncWindowResize: function() {}
    });
}

$(function() {
    if ($('.layout-wrapper').length) {
        ExecutaLayout.init({ pathname: window.location.pathname });
    }

    $('.layout-menu > li > a').each(function() {
        if (window.location.pathname === this.pathname) {
            $(this).parent('li').addClass('active-menuitem');
        }
    });
});
