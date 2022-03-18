import logging
import os

from fastapi import FastAPI

app = FastAPI()
logger = logging.getLogger("uvicorn")


def exec_cmd(command):
    out = os.system(command)
    logger.info(str(out))


@app.post('/')
def func():
    logger.info("WebHook received")
    exec_cmd("git pull")
    exec_cmd("mvn clean install")
    exec_cmd("killall java")
    return {}


if __name__ == "__main__":
    import uvicorn

    exec_cmd("git pull")
    uvicorn.run("main:app", debug=False, reload=False, host="0.0.0.0")
