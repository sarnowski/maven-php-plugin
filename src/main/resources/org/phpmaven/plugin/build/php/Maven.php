<?php
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

$testFile = $_SERVER['argv'][1];
$targetFile = $_SERVER['argv'][2];
require_once 'PHPUnit/TextUI/TestRunner.php';
require_once 'PHPUnit/Util/Log/PMD.php';
require_once 'PHPUnit/Util/Log/TAP.php';
require_once 'PHPUnit/Util/Configuration.php';
require_once 'PHPUnit/Util/Fileloader.php';
require_once 'PHPUnit/Util/Filter.php';
require_once 'PHPUnit/Util/Getopt.php';
require_once 'PHPUnit/Util/Skeleton.php';
require_once 'PHPUnit/Util/TestDox/ResultPrinter/Text.php';
require_once ($testFile);
PHPUnit_Util_Filter::addFileToFilter(__FILE__, 'PHPUNIT');
$arguments = array();
$classes = get_declared_classes();
$result = null;

for ($i=0;$i<count($classes);$i++) {
	if (strtolower(substr($classes[$i],-4,4))=='test') {
		$arguments['test'] = $classes[$i]; 
	}	
}
$arguments['testFile']=$testFile;
$arguments['xmlLogfile']=$targetFile;

$arguments['syntaxCheck']=null;
		
        $runner = new PHPUnit_TextUI_TestRunner;

        if (is_object($arguments['test']) && $arguments['test'] instanceof PHPUnit_Framework_Test) {
            $suite = $arguments['test'];
        } else {
            $suite = $runner->getTest(
              $arguments['test'],
              $arguments['testFile'],
              $arguments['syntaxCheck']
            );
        }

        if ($suite->testAt(0) instanceof PHPUnit_Framework_Warning &&
            strpos($suite->testAt(0)->getMessage(), 'No tests found in class') !== FALSE) {
            $skeleton = new PHPUnit_Util_Skeleton(
                $arguments['test'],
                $arguments['testFile']
            );

            $result = $skeleton->generate(TRUE);

            if (!$result['incomplete']) {
                eval(str_replace(array('<?php', '?>'), '', $result['code']));
                $suite = new PHPUnit_Framework_TestSuite($arguments['test'] . 'Test');
            }
        }

        try {
            $result = $runner->doRun(
              $suite,
              $arguments
            );
        }

        catch (Exception $e) {
            throw new RuntimeException(
              'Could not create and run test suite: ' . $e->getMessage()
            );
        }

        if ($result->wasSuccessful()) {
            exit(PHPUnit_TextUI_TestRunner::SUCCESS_EXIT);
        }

        else if($result->errorCount() > 0) {
            exit(PHPUnit_TextUI_TestRunner::EXCEPTION_EXIT);
        }

        else {
            exit(PHPUnit_TextUI_TestRunner::FAILURE_EXIT);
        }


?>