from setuptools import setup

setup(
    name="cerebellum",
    version="0.1.0",
    packages=['python_models',],
    package_data={'python_models.model_binaries': ['*.aplx']},
    install_requires=['SpyNNaker == 2016.001']
)
