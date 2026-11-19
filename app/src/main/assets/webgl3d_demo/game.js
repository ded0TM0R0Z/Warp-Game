(() => {
  "use strict";

  const canvas = document.getElementById("scene");
  const status = document.getElementById("status");
  const touchLabel = document.getElementById("touch-count");
  const sdk = window.WarpGameSDK || null;
  let touchCount = sdk ? Number(sdk.loadData("touch-count") || 0) : 0;
  if (!Number.isFinite(touchCount)) touchCount = 0;
  touchLabel.textContent = "Касаний: " + touchCount;

  const gl = canvas.getContext("webgl", {
    alpha: false, antialias: true, depth: true,
    powerPreference: "low-power", preserveDrawingBuffer: false
  });
  if (!gl) {
    status.textContent = "WebGL недоступен на этом устройстве";
    return;
  }

  const vertexShaderSource = `
    attribute vec3 aPosition;
    attribute vec3 aColor;
    uniform mat4 uMvp;
    varying vec3 vColor;
    void main() {
      gl_Position = uMvp * vec4(aPosition, 1.0);
      vColor = aColor;
    }
  `;
  const fragmentShaderSource = `
    precision mediump float;
    varying vec3 vColor;
    void main() {
      gl_FragColor = vec4(vColor, 1.0);
    }
  `;

  function compileShader(type, source) {
    const shader = gl.createShader(type);
    gl.shaderSource(shader, source);
    gl.compileShader(shader);
    if (!gl.getShaderParameter(shader, gl.COMPILE_STATUS)) {
      const message = gl.getShaderInfoLog(shader) || "Shader compilation failed";
      gl.deleteShader(shader);
      throw new Error(message);
    }
    return shader;
  }

  let program;
  try {
    const vertexShader = compileShader(gl.VERTEX_SHADER, vertexShaderSource);
    const fragmentShader = compileShader(gl.FRAGMENT_SHADER, fragmentShaderSource);
    program = gl.createProgram();
    gl.attachShader(program, vertexShader);
    gl.attachShader(program, fragmentShader);
    gl.linkProgram(program);
    gl.deleteShader(vertexShader);
    gl.deleteShader(fragmentShader);
    if (!gl.getProgramParameter(program, gl.LINK_STATUS)) {
      throw new Error(gl.getProgramInfoLog(program) || "Program link failed");
    }
  } catch (error) {
    status.textContent = "Ошибка 3D-шейдера";
    console.error(error);
    return;
  }

  const positions = new Float32Array([
    -1,-1,-1,  1,-1,-1,  1, 1,-1, -1, 1,-1,
    -1,-1, 1,  1,-1, 1,  1, 1, 1, -1, 1, 1
  ]);
  const colors = new Float32Array([
    0.48,0.40,1.00, 0.48,0.40,1.00, 0.72,0.66,1.00, 0.72,0.66,1.00,
    0.25,0.82,0.65, 0.25,0.82,0.65, 0.45,0.95,0.75, 0.45,0.95,0.75
  ]);
  const indices = new Uint16Array([
    0,1,2, 0,2,3, 4,6,5, 4,7,6,
    0,4,5, 0,5,1, 3,2,6, 3,6,7,
    1,5,6, 1,6,2, 0,3,7, 0,7,4
  ]);

  function bufferData(target, data) {
    const buffer = gl.createBuffer();
    gl.bindBuffer(target, buffer);
    gl.bufferData(target, data, gl.STATIC_DRAW);
    return buffer;
  }
  const positionBuffer = bufferData(gl.ARRAY_BUFFER, positions);
  const colorBuffer = bufferData(gl.ARRAY_BUFFER, colors);
  const indexBuffer = bufferData(gl.ELEMENT_ARRAY_BUFFER, indices);

  gl.useProgram(program);
  const positionLocation = gl.getAttribLocation(program, "aPosition");
  const colorLocation = gl.getAttribLocation(program, "aColor");
  const mvpLocation = gl.getUniformLocation(program, "uMvp");

  gl.bindBuffer(gl.ARRAY_BUFFER, positionBuffer);
  gl.enableVertexAttribArray(positionLocation);
  gl.vertexAttribPointer(positionLocation, 3, gl.FLOAT, false, 0, 0);
  gl.bindBuffer(gl.ARRAY_BUFFER, colorBuffer);
  gl.enableVertexAttribArray(colorLocation);
  gl.vertexAttribPointer(colorLocation, 3, gl.FLOAT, false, 0, 0);
  gl.bindBuffer(gl.ELEMENT_ARRAY_BUFFER, indexBuffer);

  function perspective(fovy, aspect, near, far) {
    const f = 1 / Math.tan(fovy / 2);
    const out = new Float32Array(16);
    out[0] = f / aspect;
    out[5] = f;
    out[10] = (far + near) / (near - far);
    out[11] = -1;
    out[14] = (2 * far * near) / (near - far);
    return out;
  }

  function rotationX(angle) {
    const c = Math.cos(angle), s = Math.sin(angle);
    return new Float32Array([
      1,0,0,0, 0,c,s,0, 0,-s,c,0, 0,0,0,1
    ]);
  }

  function rotationY(angle) {
    const c = Math.cos(angle), s = Math.sin(angle);
    return new Float32Array([
      c,0,-s,0, 0,1,0,0, s,0,c,0, 0,0,0,1
    ]);
  }

  function translationZ(distance) {
    const out = new Float32Array(16);
    out[0] = out[5] = out[10] = out[15] = 1;
    out[14] = distance;
    return out;
  }

  function multiply(a, b) {
    const out = new Float32Array(16);
    for (let column = 0; column < 4; column++) {
      for (let row = 0; row < 4; row++) {
        let sum = 0;
        for (let index = 0; index < 4; index++) {
          sum += a[index * 4 + row] * b[column * 4 + index];
        }
        out[column * 4 + row] = sum;
      }
    }
    return out;
  }

  let angleX = 0.45;
  let angleY = 0.65;
  let dragging = false;
  let previousX = 0;
  let previousY = 0;

  function resize() {
    const rect = canvas.getBoundingClientRect();
    const ratio = Math.min(window.devicePixelRatio || 1, 1.5);
    const width = Math.max(1, Math.floor(rect.width * ratio));
    const height = Math.max(1, Math.floor(rect.height * ratio));
    if (canvas.width !== width || canvas.height !== height) {
      canvas.width = width;
      canvas.height = height;
    }
    gl.viewport(0, 0, width, height);
  }

  function draw(time) {
    resize();
    gl.enable(gl.DEPTH_TEST);
    gl.disable(gl.CULL_FACE);
    gl.clearColor(0.035, 0.05, 0.09, 1);
    gl.clear(gl.COLOR_BUFFER_BIT | gl.DEPTH_BUFFER_BIT);

    const projection = perspective(0.92, canvas.width / canvas.height, 0.1, 100);
    const rotation = multiply(rotationY(angleY + time * 0.00018), rotationX(angleX));
    const model = multiply(translationZ(-5), rotation);
    const mvp = multiply(projection, model);
    gl.uniformMatrix4fv(mvpLocation, false, mvp);
    gl.drawElements(gl.TRIANGLES, indices.length, gl.UNSIGNED_SHORT, 0);
    requestAnimationFrame(draw);
  }

  canvas.addEventListener("pointerdown", event => {
    dragging = true;
    previousX = event.clientX;
    previousY = event.clientY;
    canvas.setPointerCapture(event.pointerId);
    touchCount += 1;
    touchLabel.textContent = "Касаний: " + touchCount;
    if (sdk) {
      sdk.saveData("touch-count", touchCount);
      sdk.vibrate(12);
    }
  });

  canvas.addEventListener("pointermove", event => {
    if (!dragging) return;
    const dx = event.clientX - previousX;
    const dy = event.clientY - previousY;
    previousX = event.clientX;
    previousY = event.clientY;
    angleY += dx * 0.008;
    angleX += dy * 0.008;
  });

  const stopDragging = () => { dragging = false; };
  canvas.addEventListener("pointerup", stopDragging);
  canvas.addEventListener("pointercancel", stopDragging);
  window.addEventListener("resize", resize, { passive: true });
  canvas.addEventListener("webglcontextlost", event => {
    event.preventDefault();
    status.textContent = "WebGL-контекст потерян";
  });

  status.textContent = gl.getParameter(gl.VERSION) + " · " +
    gl.getParameter(gl.RENDERER);
  requestAnimationFrame(draw);
})();