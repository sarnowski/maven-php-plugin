/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.phpmaven.plugin.build.php;

import java.io.File;

/**
 * @author Christian Wiedemann
 * @author Tobias Sarnowski
 */
public class PhpExecutionException extends PhpException {

    private final String phpErrorMessage;
    private final File phpFile;

    public PhpExecutionException(File phpFile, String phpErrorMessage) {
        this.phpFile = phpFile;
        this.phpErrorMessage = "\n" + phpErrorMessage;
    }

    public String getMessage() {
        if (phpFile != null) {
            return phpErrorMessage + "\nin file: " + phpFile.getAbsolutePath();
        } else {
            return phpErrorMessage;
        }
    }
}
