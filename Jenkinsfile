/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

// It's assumed that Jenkins has been configured to implicitly load the vars/xwikiModule.groovy library which exposes
// the "xwikiModule" global function/DSL.
// Note that the version used is the one defined in Jenkins but it can be overridden as follows:
// @Library("XWiki@<branch, tag, sha1>") _
// See https://github.com/jenkinsci/workflow-cps-global-lib-plugin for details.

parallel(
    'current': {
        node() {
            xwikiBuild('current') {
                // Default configuration
            }
        }
    },
    'latest': {
        node() {
            xwikiBuild('latest') {
                // Upgrade parents to build with the latest version of XWiki available to ensure that this extension
                // works with the latest XWiki.
                updateParent: true
                // Note: we use the "verify" goals to be sure we don't install or deploy artifacts to any Maven repo
                // (which would overwrite proper artifacts!).
                goals: 'verify'
            }
        }
    }
)

