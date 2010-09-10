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

/**
 * Something with PHP is wrong.
 *
 * @author Tobias Sarnowski
 */
public abstract class PhpException extends Exception {

    // checkstyle wants exceptions to be immutable but we
    // can not implement that at the moment
    /*CHECKSTYLE:OFF*/
    private String phpOutput;
    /*CHECKSTYLE:ON*/

    protected PhpException() {
        super();
    }

    protected PhpException(String message) {
        super(message);
    }

    protected PhpException(String message, Throwable cause) {
        super(message, cause);
    }

    protected PhpException(Throwable cause) {
        super(cause);
    }

    /**
     * Adds php outputs to the exception.
     *
     * @param output the output string
     */
    public void appendOutput(String output) {
        this.phpOutput = output;
    }

    /**
     * Returns the given output.
     *
     * @return the output string
     */
    public String getAppendedOutput() {
        return phpOutput;
    }

    @Override
    public String getMessage() {
        String message = super.getMessage();
        if (phpOutput != null) {
            message = message + "\n\n" + phpOutput;
        }
        return message;
    }
}
