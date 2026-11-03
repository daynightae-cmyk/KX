"""Privacy/diagnostic regression; no Android run is implied."""
from pathlib import Path
import tempfile
import unittest
from summarize_android_test_failures import summarize


class AndroidFailureSummaryTest(unittest.TestCase):
    def test_only_exception_types_and_source_frames_escape_report(self):
        with tempfile.TemporaryDirectory(prefix="kx-summary-") as folder:
            root = Path(folder)
            (root / "TEST-example.xml").write_text('''<testsuite>
<testcase classname="com.kx.vpn.LaunchInstrumentedTest" name="secret-case-name">
<failure>java.lang.IllegalStateException: secret failure value
 at com.kx.vpn.LaunchInstrumentedTest.captureLaunch(LaunchInstrumentedTest.kt:233)
Caused by: java.lang.NullPointerException: secret cause value
&lt;ui-hierarchy&gt;secret screen content&lt;/ui-hierarchy&gt;
</failure></testcase></testsuite>''')
            output = "\n".join(summarize(root))
            self.assertIn("java.lang.IllegalStateException", output)
            self.assertIn("java.lang.NullPointerException", output)
            self.assertIn("LaunchInstrumentedTest.kt:233", output)
            self.assertNotIn("secret", output)
            self.assertNotIn("ui-hierarchy", output)

    def test_malformed_or_absent_reports_do_not_dump_contents(self):
        with tempfile.TemporaryDirectory(prefix="kx-summary-") as folder:
            root = Path(folder)
            self.assertIn("No structured", summarize(root)[0])
            (root / "TEST-invalid.xml").write_text("sensitive not xml")
            output = "\n".join(summarize(root))
            self.assertIn("unreadable", output)
            self.assertNotIn("sensitive", output)

    def test_untrusted_class_name_is_withheld(self):
        with tempfile.TemporaryDirectory(prefix="kx-summary-") as folder:
            root = Path(folder)
            (root / "TEST-invalid-class.xml").write_text('''<testsuite><testcase classname="private value"><error>unknown text</error></testcase></testsuite>''')
            output = "\n".join(summarize(root))
            self.assertIn("[withheld]", output)
            self.assertNotIn("private value", output)


if __name__ == "__main__":
    unittest.main()
