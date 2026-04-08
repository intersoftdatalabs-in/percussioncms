/*
 * Copyright 1999-2023 Percussion Software, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

var requireJsConfig = {
    baseUrl: '',
    //urlArgs: "bust=build-1.2.34",
    paths: {
        'components': '/cm/cui/components',
        'text': '/cm/cui/components/requirejs-text/text',
        'jquery': '/cm/jslib/profiles/3x/jquery/jquery-3.6.0',
        'jquery-migrate': '/cm/jslib/profiles/3x/jquery/jquery-migrate-3.3.2',
        'jquery-ui': '/cm/cui/components/jquery-ui/jquery-ui',
        'knockout': '/cm/cui/components/knockoutjs/dist/knockout',
        'pubsub': '/cm/cui/components/pubsub-js/src/pubsub',
        'fancytree': '/cm/jslib/profiles/3x/jquery/plugins/jquery-fancytree/jquery.fancytree.min',
        'widgel-base': '/cm/cui/components/widgel/dist/widgel-base',
        'perc-utils': '/cm/cui/components/perc-utils/dist',
        'perc-css': '/cm/cui/components/perc-css/perc.css',
        'opensans-css': '/cm/cui/components/google-fonts/opensans.css',
        'montserrat-css': '/cm/cui/components/google-fonts/montserrat.css',
        'fancytree-css':'/cm/jslib/profiles/3x/jquery/plugins/jquery-fancytree/skin-themeroller/ui.fancytree.min.css',
        'widgets': '/cm/cui/widgets',
        'modules': '/cm/cui/modules',
        'test'   : '/cm/cui/test',
        'utils'  : '/cm/cui/pages/utils',
        'css': 'css',
        'fontawesome-css': '/cm/jslib/profiles/3x/libraries/fontawesome/css/all.css',
        'bootstrap':'/cm/jslib/profiles/3x/libraries/bootstrap/css/bootstrap.min.css',
        'bootstrap-theme':'/cm/cui/components/twitter-bootstrap-3.0.0/dist/css/bootstrap-theme.min.css'
    },
    // Map all fancytree's individual jquery-ui sub-module dependencies to the
    // bundled jquery-ui module. The bundled jquery-ui.js includes all required
    // sub-modules (widget, position, draggable, droppable, mouse, etc.).
    // Also map fancytree's internal ui-deps shim to the bundled jquery-ui.
    map: {
        '*': {
            'jquery.fancytree.ui-deps':         'jquery-ui',
            'jquery-ui/ui/widget':              'jquery-ui',
            'jquery-ui/ui/data':                'jquery-ui',
            'jquery-ui/ui/scroll-parent':       'jquery-ui',
            'jquery-ui/ui/focusable':           'jquery-ui',
            'jquery-ui/ui/keycode':             'jquery-ui',
            'jquery-ui/ui/position':            'jquery-ui',
            'jquery-ui/ui/tabbable':            'jquery-ui',
            'jquery-ui/ui/unique-id':           'jquery-ui',
            'jquery-ui/ui/effect':              'jquery-ui',
            'jquery-ui/ui/effects/effect-blind':'jquery-ui',
            'jquery-ui/ui/widgets/mouse':       'jquery-ui',
            'jquery-ui/ui/widgets/draggable':   'jquery-ui',
            'jquery-ui/ui/widgets/droppable':   'jquery-ui'
        }
    },
    shim: {
        'knockout': {
            exports: 'ko'
        },
        'jquery-ui': {
            exports: '$',
            deps: ['jquery']
        },
        'fancytree': {
            deps: ['jquery-ui']
        }
    }
};

if (typeof(exports) !== 'undefined' && exports !== null) {
    exports.requireJsConfig = requireJsConfig;
}
