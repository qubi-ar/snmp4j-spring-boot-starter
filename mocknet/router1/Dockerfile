FROM python:3.11-slim
RUN pip install --no-cache-dir snmpsim
RUN addgroup --system snmpsim && adduser --system --ingroup snmpsim snmpsim
USER snmpsim
WORKDIR /data
CMD ["snmpsim-command-responder",\
    "--data-dir=/data",\
    "--log-level","info",\
    "--agent-udpv4-endpoint=0.0.0.0:1611"]
