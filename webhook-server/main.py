import logging
import os
import threading

from fastapi import FastAPI

app = FastAPI()
logger = logging.getLogger("uvicorn")


def exec_cmd(command):
    out = os.system(command)
    logger.info(str(out))


def sync_and_build():
    logger.info("WebHook received")
    exec_cmd("git pull")
    exec_cmd("mvn clean install")
    exec_cmd("killall java")


@app.post('/')
def func():
    task = threading.Thread(target=sync_and_build)
    task.start()
    return {}


if __name__ == "__main__":
    import uvicorn

    exec_cmd("git pull")
    uvicorn.run("main:app", debug=False, reload=False, host="0.0.0.0")
