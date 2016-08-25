#!/usr/bin/env python

import atexit
import mdk
import os
import time

from flask import Flask, jsonify
app = Flask(__name__)


m = mdk.init()

service_name = os.getenv('MDK_SERVICE_NAME')
service_vers = os.getenv('MDK_SERVICE_VERSION')
m.register(service_name, service_vers, "http://{}:{}".format(os.getenv('DATAWIRE_ROUTABLE_HOST'), 5000))

m.start()

atexit.register(m.stop)


@app.route('/hello/<name>', methods=['GET'])
def hello(name):
    return jsonify(message="Hello {} from a Mobius deployed service!".format(name),
                   time=int(round(time.time() * 1000)))


@app.route('/health', methods=['GET', 'HEAD'])
def health():
    # TODO: Custom health check logic.
    return '', 200


def main():
    app.run(debug=True)


if __name__ == '__main__':
    main()
