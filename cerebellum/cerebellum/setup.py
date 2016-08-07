from setuptools import setup

setup(
    name="cerebellum",
    version="0.1.0",
    packages=['cerebellum',],
    package_data={'cerebellum.model_binaries': ['*.aplx']},
    install_requires=['SpyNNaker == 2016.001']
)
