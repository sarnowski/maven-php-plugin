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

package org.phpmaven.plugin.build;

import java.io.File;

/**
 * Symbolizes an error, printed out by a PHP execution.
 *
 * @author Tobias Sarnowski
 */
public class PhpErrorException extends PhpExecutionException {

    /**
     * Creates an exception for the occured error.
     *
     * @param phpFile the PHP file which was involved in the exception
     * @param phpErrorMessage the error message
     */
    public PhpErrorException(File phpFile, String phpErrorMessage) {
        super(phpFile, phpErrorMessage);
    }
}
