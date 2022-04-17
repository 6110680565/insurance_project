const { urlencoded } = require("body-parser");
const express = require("express");
const app = express();
const listOfCustomer = [{"name":"nut", "countClaim": 2  },{"name":"tham", "countClaim": 0  }];

app.get('/getcount/:name', (req, res) => {
  let name = req.params.name;
  let data = {"name":"notfind"};
  listOfCustomer.forEach(element => {
    if (element["name"] == name){
      console.log(element);
      data = element;
    }
  });

  res.json(data);
  
  
  
});


app.listen(3000, () => {
  console.log("running server");
});

//URL("localhost:3000/getcount/nut") ==> {"name":"nut", "countClaim": 2  }