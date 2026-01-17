#############################
# PROVIDER
#############################

provider "aws" {
  region = "eu-central-1"
}

#############################
# AMI
#############################

data "aws_ami" "ubuntu" {
  most_recent = true

  filter {
    name   = "name"
    values = ["ubuntu/images/hvm-ssd/ubuntu-jammy-22.04-amd64-server-*"]
  }

  owners = ["099720109477"]
}

#############################
# SECURITY GROUP
#############################

resource "aws_security_group" "qa_sg" {
  name_prefix = "qa-environment-"
  description = "QA ephemeral environment security group"

  ingress {
    description = "SSH"
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "Application access"
    from_port   = 4173
    to_port     = 4173
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

#############################
# EC2 INSTANCE
#############################

resource "aws_instance" "qa_vm" {
  ami                    = data.aws_ami.ubuntu.id
  instance_type          = "t2.micro"
  key_name               = "ssh-aws-test"
  vpc_security_group_ids = [aws_security_group.qa_sg.id]

  tags = {
    Name = "qa-environment-${terraform.workspace}"
  }
}

#############################
# OUTPUT
#############################

output "app_public_ip" {
  description = "Public IP of the QA EC2 instance"
  value       = aws_instance.qa_vm.public_ip
}
