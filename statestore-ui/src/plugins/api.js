import axios from 'axios'
import VueAxios from 'vue-axios'
import Vue from "vue"; //this line is important to remove 'protocol' ERROR

Vue.use(VueAxios, axios)

let apiUrl = window.location.protocol + "//" + window.location.hostname + ":8888";
//let apiUrl = "http://localhost:8080";

axios.interceptors.response.use(
  res => {
    for (var h in req.headers) {
      console.log("header: " + h + " : " + req.headers[h]);
    }
    return res;
  },
  err => {
      console.log("error: " + err.response.status);
      throw err;
  }
);

let api = axios.create({
    // baseURL: apiUrl,
    baseURL: apiUrl,
    headers: {
        'Content-Type': 'application/json'
    }
});

export default api