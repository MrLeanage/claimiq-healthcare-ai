"""Absolute-path entry point used to identify this repository's managed process."""
import runpy

if __name__ == "__main__":
    runpy.run_module("app.server", run_name="__main__")
